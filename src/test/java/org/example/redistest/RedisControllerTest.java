package org.example.redistest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * test controller http processing is correct
 */
@Tag("unit-test")
@ExtendWith(SpringExtension.class)
//@RunWith(SpringRunner.class)
@WebMvcTest(value = RedisController.class)
public class RedisControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private RedisService redisService;

    @Test
    public void internal_server_error_should_return_http_500() throws Exception {
        Mockito.when(redisService.getLast()).thenThrow(new RuntimeException("wowoww!"));
        mvc.perform(get("/last"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void ensure_publish_returns_http_204() throws Exception {
        mvc.perform(post("/publish")
                .contentType(MediaType.TEXT_PLAIN)
                .content("message"))
                .andDo(print())
                .andExpect(status().isNoContent());

        Mockito.verify(redisService).publish("message");
    }

    @Test
    public void ensure_empty_last_message_returns_http_204() throws Exception {
        mvc.perform(get("/last"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void ensure_last_returns_message_and_http_200() throws Exception {
        Mockito.when(redisService.getLast()).thenReturn("message");

        mvc.perform(get("/last"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("message"));
    }

    @Test
    public void ensure_time_instant_q_params_and_resulting_list_of_messages_are_parsed() throws Exception {
        Instant t1 = Instant.parse("2018-11-29T18:35:24.003Z");
        Instant t2 = Instant.parse("2018-11-30T18:35:24.003Z");

        List<String> msgs = Arrays.asList("m1", "m2", "m3");
        Mockito.when(redisService.getByTime(any(), any())).thenReturn(msgs);

        MvcResult result = mvc.perform(
                get("/by-time")
                        .queryParam("start", DateTimeFormatter.ISO_INSTANT.format(t1))
                        .queryParam("end", DateTimeFormatter.ISO_INSTANT.format(t2))
                        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        List<String> strings = json.readValue(result.getResponse().getContentAsString(), new TypeReference<List<String>>() {});
        Assertions.assertEquals(msgs, strings);
    }
}
