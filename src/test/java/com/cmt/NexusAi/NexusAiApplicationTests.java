//package com.cmt.NexusAi;
//
//import cn.hutool.core.util.RandomUtil;
//import com.cmt.NexusAi.modules.security.constant.UserConstant;
//import com.cmt.NexusAi.modules.user.model.entity.User;
//import com.cmt.NexusAi.modules.user.service.UserService;
//import jakarta.annotation.Resource;
//import org.springframework.http.MediaType;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.io.FileWriter;
//import java.io.PrintWriter;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@DirtiesContext
//class NexusAiApplicationTests {
//
//
//    @Resource
//    private UserService userService;
//
//    @Resource
//    private MockMvc mockMvc;
//
//
//
//    @Test
//    void testLoginAndExportSessionToCsv() throws Exception {
//        List<User> list = userService.list();
//
//        try (PrintWriter writer = new PrintWriter(new FileWriter("session_output.csv", false))) {
//            writer.println("userId,sessionId,timestamp");
//
//            int successCount = 0;
//            int failCount = 0;
//
//            for (User user : list) {
//                long testUserId = user.getId();
//
//                for (int retry = 0; retry < 3; retry++) {
//                    try {
//                        MvcResult result = mockMvc.perform(get("/user/login")
//                                        .param("userId", String.valueOf(testUserId))
//                                )
//                                .andReturn();
//
//                        // ✅ 直接从 Response 的 Set-Cookie 里拿 SESSION ID
//                        List<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
//                        if (setCookieHeaders.isEmpty()) {
//                            throw new RuntimeException("No Set-Cookie header");
//                        }
//
//                        String sessionId = setCookieHeaders.stream()
//                                .filter(cookie -> cookie.startsWith("SESSION="))
//                                .map(cookie -> cookie.split(";")[0].split("=")[1])
//                                .findFirst()
//                                .orElseThrow(() -> new RuntimeException("No SESSION found"));
//
//                        writer.printf("%d,%s,%s%n", testUserId, sessionId, LocalDateTime.now());
//                        successCount++;
//
//                        if (successCount % 1000 == 0) {
//                            System.out.println("✅ 已完成: " + successCount + " / " + list.size());
//                        }
//
//                        break;
//
//                    } catch (Exception e) {
//                        failCount++;
//                        System.err.println("❌ 第" + (retry+1) + "次失败: userId=" + testUserId + ", 错误: " + e.getMessage());
//
//                        if (retry == 2) {
//                            System.err.println("❌ 最终失败: userId=" + testUserId);
//                        } else {
//                            Thread.sleep(100);
//                        }
//                    }
//                }
//            }
//
//            System.out.println("🎉 生成完成！成功: " + successCount + ", 失败: " + failCount);
//        }
//    }
//
////    @Test
////    void testLoginAndExportSessionToCsv() throws Exception {
////        List<User> list = userService.list();
////
////        try (PrintWriter writer = new PrintWriter(new FileWriter("session_output.csv", true))) {
////            // 如果文件是第一次写入，可以加一个逻辑写表头
////            writer.println("userId,sessionId,timestamp");
////
////            for (User user : list) {
////                long testUserId = user.getId();
////
////                MvcResult result = mockMvc.perform(get("/user/login")
////                                .param("userId", String.valueOf(testUserId)))  // ✅ 删掉 contentType，其他全留
////                        .andReturn();  // ✅ 这个必须留，不能删！
////
////
////                List<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
////                assertThat(setCookieHeaders).isNotEmpty();
////
////                String sessionId = setCookieHeaders.stream()
////                        .filter(cookie -> cookie.startsWith("SESSION")) // Spring Session 默认是 SESSION（不是 JSESSIONID）
////                        .map(cookie -> cookie.split(";")[0]) // SESSION=xxx
////                        .findFirst()
////                        .orElseThrow(() -> new RuntimeException("No SESSION found in response"));
////
////                String sessionValue = sessionId.split("=")[1];
////
////                writer.printf("%d,%s,%s%n", testUserId, sessionValue, LocalDateTime.now());
////
////                System.out.println("✅ 写入 CSV：" + testUserId + " -> " + sessionValue);
////            }
////        }
////    }
//
//    @Test
//    void addUser() {
//        for (int i = 0; i < 50000; i++) {
//            User user = new User();
//            user.setUsername(RandomUtil.randomString(6));
//            userService.save(user);
//        }
//    }
//
//
//}
