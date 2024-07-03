package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        long delay = timeUnit.toMillis(1);
        this.scheduler.scheduleAtFixedRate(semaphore::release, delay, delay, TimeUnit.MILLISECONDS);
    }

    public int createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(document), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Signature", signature)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.code();
            }
        } finally {
            semaphore.release();
        }
    }

    public Document jsonToDocument(String json) throws IOException {
        return objectMapper.readValue(json, Document.class);
    }

    public static class Document {
        public Description description;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public List<Product> products;
        public String regDate;
        public String regNumber;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificateDocument;
            public String certificateDocumentDate;
            public String certificateDocumentNumber;
            public String ownerInn;
            public String producerInn;
            public String productionDate;
            public String tnvedCode;
            public String uitCode;
            public String uituCode;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        String json = """
        {
            "docId": "12345",
            "docStatus": "DRAFT",
            "docType": "LP_INTRODUCE_GOODS",
            "importRequest": true,
            "ownerInn": "000000000000",
            "participantInn": "111111111111",
            "producerInn": "222222222222",
            "productionDate": "2020-01-23",
            "productionType": "SAMPLE_TYPE",
            "regDate": "2020-01-23",
            "regNumber": "12345",
            "description": {
                "participantInn": "111111111111"
            },
            "products": [
                {
                    "certificateDocument": "DOC123",
                    "certificateDocumentDate": "2020-01-23",
                    "certificateDocumentNumber": "12345",
                    "ownerInn": "000000000000",
                    "producerInn": "222222222222",
                    "productionDate": "2020-01-23",
                    "tnvedCode": "0000000000",
                    "uitCode": "UIT123",
                    "uituCode": "UITU123"
                }
            ]
        }
        """;
        String signature = "sample-signature";

        Document document = api.jsonToDocument(json);
        int responseCode = api.createDocument(document, signature);
        System.out.println(responseCode);
    }
}
