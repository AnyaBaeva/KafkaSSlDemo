package com.example.client;

import com.example.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ProductParser {

    private static final String USER_QUERY_TOPIC = "userQuery";
    private static final String RESPONSE_TOPIC = "response";

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static KafkaProducer<String, String> kafkaProducer;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Использование: java ProductParser <filePath> <brandFilter> <maxMonths> [kafkaBootstrapServers]");
            System.err.println("Пример: java ProductParser product-final.json XYZ 1");
            System.exit(1);
        }

        String filePath = args[0];
        String brandFilter = args[1];
        int maxMonths;

        try {
            maxMonths = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("maxMonths должен быть числом: " + args[2]);
            System.exit(1);
            return;
        }

        try {
            initializeKafkaProducer();

            // Чтение и парсинг данных
            List<Product> products = readProductsFromFile(filePath);

            // Фильтрация данных
            List<Product> filteredProducts = filterProducts(products, brandFilter, maxMonths);

            // Логирование запроса пользователя
            logUserQuery(brandFilter, maxMonths, filteredProducts.size());

            // Отправка результатов в Kafka
            sendResultsToKafka(filteredProducts);

            System.out.println("Обработано продуктов: " + filteredProducts.size());
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (kafkaProducer != null) {
                kafkaProducer.close();
            }
        }
    }

    private static List<Product> readProductsFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Файл не найден: " + filePath);
        }

        List<Product> products = new ArrayList<>();
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                try {
                    Product product = parseProductFromString(line);
                    products.add(product);
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга строки: " + line);
                    e.printStackTrace();
                }
            }
        }
        scanner.close();

        return products;
    }

    private static Product parseProductFromString(String line) {
        String content = line.substring(1, line.length() - 1).trim();

        Product product = new Product();
        Map<String, String> fields = parseKeyValuePairs(content);

        // Заполняем поля продукта
        product.setStoreId(fields.get("store_id"));
        product.setDescription(fields.get("description"));
        product.setCreatedAt(parseDateTime(fields.get("created_at")));
        product.setIndex(fields.get("index"));
        product.setTags(parseList(fields.get("tags")));
        product.setUpdatedAt(parseDateTime(fields.get("updated_at")));
        product.setProductId(fields.get("product_id"));
        product.setName(fields.get("name"));
        product.setCategory(fields.get("category"));
        product.setSku(fields.get("sku"));
        product.setBrand(fields.get("brand"));

        // Обрабатываем вложенные объекты
        product.setImages(parseImages(fields.get("images")));
        product.setSpecifications(parseSpecifications(fields.get("specifications")));
        product.setPrice(parsePrice(fields.get("price")));
        product.setStock(parseStock(fields.get("stock")));

        return product;
    }

    private static Map<String, String> parseKeyValuePairs(String content) {
        Map<String, String> result = new HashMap<>();
        int braceLevel = 0;
        int bracketLevel = 0;
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean inKey = true;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '{') braceLevel++;
            if (c == '}') braceLevel--;
            if (c == '[') bracketLevel++;
            if (c == ']') bracketLevel--;

            if (c == ',' && braceLevel == 0 && bracketLevel == 0) {
                // Завершаем текущую пару ключ-значение
                if (currentKey.length() > 0 && currentValue.length() > 0) {
                    result.put(currentKey.toString().trim(), currentValue.toString().trim());
                }
                currentKey.setLength(0);
                currentValue.setLength(0);
                inKey = true;
                continue;
            }

            if (c == '=' && braceLevel == 0 && bracketLevel == 0 && inKey) {
                inKey = false;
                continue;
            }

            if (inKey) {
                currentKey.append(c);
            } else {
                currentValue.append(c);
            }
        }

        // Добавляем последнюю пару
        if (currentKey.length() > 0 && currentValue.length() > 0) {
            result.put(currentKey.toString().trim(), currentValue.toString().trim());
        }

        return result;
    }

    private static List<Image> parseImages(String imagesString) {
        List<Image> images = new ArrayList<>();
        if (imagesString == null || !imagesString.startsWith("[")) {
            return images;
        }

        // Убираем обрамляющие []
        String content = imagesString.substring(1, imagesString.length() - 1).trim();
        List<String> imageStrings = splitByComma(content, '{', '}');

        for (String imageStr : imageStrings) {
            if (imageStr.startsWith("{")) {
                Map<String, String> imageFields = parseKeyValuePairs(imageStr.substring(1, imageStr.length() - 1));
                Image image = new Image();
                image.setAlt(imageFields.get("alt"));
                image.setUrl(imageFields.get("url"));
                images.add(image);
            }
        }

        return images;
    }

    private static Specifications parseSpecifications(String specString) {
        if (specString == null || !specString.startsWith("{")) {
            return null;
        }

        Map<String, String> specFields = parseKeyValuePairs(specString.substring(1, specString.length() - 1));
        Specifications specs = new Specifications();
        specs.setWaterResistance(specFields.get("water_resistance"));
        specs.setWeight(specFields.get("weight"));
        specs.setBatteryLife(specFields.get("battery_life"));
        specs.setDimensions(specFields.get("dimensions"));
        return specs;
    }

    private static Price parsePrice(String priceString) {
        if (priceString == null || !priceString.startsWith("{")) {
            return null;
        }

        Map<String, String> priceFields = parseKeyValuePairs(priceString.substring(1, priceString.length() - 1));
        Price price = new Price();
        price.setAmount(Double.parseDouble(priceFields.get("amount")));
        price.setCurrency(priceFields.get("currency"));
        return price;
    }

    private static Stock parseStock(String stockString) {
        if (stockString == null || !stockString.startsWith("{")) {
            return null;
        }

        Map<String, String> stockFields = parseKeyValuePairs(stockString.substring(1, stockString.length() - 1));
        Stock stock = new Stock();
        stock.setAvailable(Integer.parseInt(stockFields.get("available")));
        stock.setReserved(Integer.parseInt(stockFields.get("reserved")));
        return stock;
    }

    private static List<String> parseList(String listString) {
        List<String> result = new ArrayList<>();
        if (listString == null || !listString.startsWith("[")) {
            return result;
        }

        // Убираем обрамляющие []
        String content = listString.substring(1, listString.length() - 1).trim();
        String[] items = content.split(", ");
        for (String item : items) {
            result.add(item.trim());
        }
        return result;
    }

    private static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null) {
            return null;
        }
        String cleanString = dateTimeString.replace("\"", "");
        return ZonedDateTime.parse(cleanString).toLocalDateTime();
    }

    // Вспомогательный метод для разделения с учетом вложенных структур
    private static List<String> splitByComma(String content, char openChar, char closeChar) {
        List<String> result = new ArrayList<>();
        int level = 0;
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == openChar) level++;
            if (c == closeChar) level--;

            if (c == ',' && level == 0) {
                result.add(content.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(content.substring(start).trim());
        return result;
    }

    private static List<Product> filterProducts(List<Product> products, String brand, int maxMonths) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(maxMonths, ChronoUnit.MONTHS);

        return products.stream()
                .filter(p -> brand == null || brand.equalsIgnoreCase(p.getBrand()))
                .filter(p -> p.getUpdatedAt() != null &&
                        p.getUpdatedAt().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    private static void initializeKafkaProducer() {
        Properties props = KafkaProperties.getConfig();
        kafkaProducer = new KafkaProducer<>(props);
    }

    private static void logUserQuery(String brand, int maxMonths, int resultCount) throws IOException {
        Map<String, Object> queryLog = new HashMap<>();
        queryLog.put("timestamp", LocalDateTime.now().toString());
        queryLog.put("brand_filter", brand);
        queryLog.put("max_months", maxMonths);
        queryLog.put("result_count", resultCount);

        String queryJson = objectMapper.writeValueAsString(queryLog);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(USER_QUERY_TOPIC, queryJson);

        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Ошибка отправки запроса: " + exception.getMessage());
            } else {
                System.out.println("Запрос записан в топик " + USER_QUERY_TOPIC);
            }
        });
    }

    private static void sendResultsToKafka(List<Product> products) throws IOException {
        int successCount = 0;
        int errorCount = 0;

        for (Product product : products) {
            String productJson = objectMapper.writeValueAsString(product);
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(RESPONSE_TOPIC, product.getProductId(), productJson);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Ошибка отправки продукта: " +
                            product.getProductId() + " - " + exception.getMessage());
                }
            });
        }

        kafkaProducer.flush();
        System.out.println("Результаты отправлены в топик " + RESPONSE_TOPIC);
    }
}