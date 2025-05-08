package com.example.announcement.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AnnouncementController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @GetMapping("/getInfo")
    public ResponseEntity<?> getInfo() {
        String szseUrl = "https://www.szse.cn/api/disc/announcement/annList?random=0.47085534213338676";
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            // 请求参数
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("bigIndustryCode", Arrays.asList("C"));
            requestPayload.put("channelCode", Arrays.asList("listedNotice_disc"));
            requestPayload.put("pageNum", 1);
            requestPayload.put("pageSize", 50);
//            requestPayload.put("seDate", Arrays.asList(LocalDate.now().toString(), LocalDate.now().toString()));
            requestPayload.put("seDate", Arrays.asList("2025-05-07", LocalDate.now().toString()));
            requestPayload.put("searchKey", Arrays.asList("汽车"));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestPayload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(szseUrl, requestEntity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody()).path("data");
            if (!root.isArray()) return ResponseEntity.status(500).body("数据格式异常");
            int count = 0;
            for (JsonNode item : root) {
                if (count++ >= 5) break;
                Map<String, Object> ann = new HashMap<>();
                ann.put("companyName", item.path("secName").get(0).asText());
                ann.put("companyCode", item.path("secCode").get(0).asText());
                ann.put("title", item.path("title").asText());
                ann.put("publishTime", item.path("publishTime").asText());
                ann.put("id", item.path("annId").asText());
                ann.put("attachPath", "https://disc.static.szse.cn" + item.path("attachPath").asText());
                // 调用 Dify 接口
                String difyUrl = "http://172.16.91.138/v1/workflows/run";
                Map<String, Object> difyPayload = new HashMap<>();
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("url", ann.get("attachPath"));
                difyPayload.put("inputs", inputs);
                difyPayload.put("response_mode", "blocking");
                difyPayload.put("user", "abc-123");
                HttpHeaders difyHeaders = new HttpHeaders();
                difyHeaders.setContentType(MediaType.APPLICATION_JSON);
                difyHeaders.setBearerAuth("app-FCZ0S9tqpn4LCNgaBJK6tnaH");
                HttpEntity<String> difyRequest = new HttpEntity<>(objectMapper.writeValueAsString(difyPayload), difyHeaders);
                ResponseEntity<String> difyResponse = restTemplate.postForEntity(difyUrl, difyRequest, String.class);
                String report = objectMapper.readTree(difyResponse.getBody())
                        .path("data").path("outputs").path("text").asText("");
                ann.put("report", report);
                System.out.println("========== " + ann.get("title") + " ==========");
                System.out.println(report);
                resultList.add(ann);

            }
            // 写 Excel
            writeExcel(resultList);
            return ResponseEntity.ok(resultList);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("错误: " + e.getMessage());
        }


    }
    private void writeExcel(List<Map<String, Object>> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Announcements");

            // 标题行
            XSSFRow header = sheet.createRow(0);
            String[] columns = {"companyName", "companyCode", "title", "publishTime", "id", "attachPath", "report"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            // 内容行
            for (int i = 0; i < data.size(); i++) {
                XSSFRow row = sheet.createRow(i + 1);
                Map<String, Object> ann = data.get(i);
                for (int j = 0; j < columns.length; j++) {
                    row.createCell(j).setCellValue(String.valueOf(ann.get(columns[j])));
                }
            }
            try (FileOutputStream out = new FileOutputStream("announList.xlsx")) {
                workbook.write(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
