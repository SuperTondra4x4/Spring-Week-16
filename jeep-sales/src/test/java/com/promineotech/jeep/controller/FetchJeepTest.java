package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;
import lombok.Getter;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
    "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
    config = @SqlConfig(encoding = "utf-8"))

class FetchJeepTest extends FetchJeepTestSupport {
  
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = {
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
      config = @SqlConfig(encoding = "utf-8"))
  class TestsThatDoNotPolluteTheApplicationContext extends FetchJeepTestSupport {
    @Test
    void testThatAJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
      
      System.out.println("hi");
  // given: a valid model, trim and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Sport";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

  // when: a connection is made to the URI
//      ResponseEntity<Jeep> response = getRestTemplate().getForEntity(uri, Jeep.class);
      ResponseEntity<List<Jeep>> response = getRestTemplate().exchange(uri, 
          HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      
  // then: a success (OK - 200) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

//      And: the actual list returned is the same as the expected list
      List<Jeep> actual = response.getBody();
      List<Jeep> expected = buildExpected();
      actual.forEach(jeep -> jeep.setModelPK(null));
      assertThat(actual).isEqualTo(expected);
      }
    @Test
    void testThatAnErrorMessageIsRetrunedWhenABadValueTrimIsSupplied() {
      System.out.println("hi");
      
  // given: a valid model, trim and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Invalid";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

  // when: a connection is made to the URI
//      ResponseEntity<Jeep> response = getRestTemplate().getForEntity(uri, Jeep.class);
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, 
          HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      
  // then: a 404 status could is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

//      And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
      }
    @ParameterizedTest
    @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
    void testThatAnErrorMessageIsRetrunedWhenAnInvalidTrimIsSupplied(String model, String trim, String reason) {
      
      System.out.println("bad request test");
      
  // given: a valid model, trim and URI
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

  // when: a connection is made to the URI
//      ResponseEntity<Jeep> response = getRestTemplate().getForEntity(uri, Jeep.class);
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, 
          HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      
  // then: a 404 status could is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

//      And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);

      }
  }
    static Stream<Arguments> parametersForInvalidInput() {
      return Stream.of(
          arguments("WRANGLER", "asdhfjgasdfhgasdkfgkjasdfkjhgasdfjkhgasdfkjhgasdfkjhgasdfkjhgasdfkjhgasdfkjhgasdfkjhg", "Trim too long"),
          arguments("WRANGLER", "!@#^*&@!^#*!&@^#*&", "Trim contains non-alpha-numeric characters"),
          arguments("INVALID", "Sport", "Model is not enum value")
          );
    }
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = {
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
      config = @SqlConfig(encoding = "utf-8"))
  class TestsThatPolluteTheApplicationContext extends FetchJeepTestSupport { 
    @MockBean
    private JeepSalesService jeepSalesService;
    @Test
    void testThatAnUnplannedErrorResultsInA500Status() {
      
      System.out.println("hi");
      
  // given: a valid model, trim and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "INvalid";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);
      doThrow(new RuntimeException("Ouch!")).when(jeepSalesService).fetchJeeps(model, trim);
      
  // when: a connection is made to the URI
//      ResponseEntity<Jeep> response = getRestTemplate().getForEntity(uri, Jeep.class);
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, 
          HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      
      System.out.println(response.getBody());
      
  // then: An internal server erro (500) is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

//      And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  @Autowired
  private JdbcTemplate jdbcTemplate;
//  @Test
//  void testDb() {
//    int numrows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "customers");
//    System.out.println("num=" + numrows);
//  }
  
//  @Disabled
//  @Autowired
//  @Getter
//  
//  private TestRestTemplate restTemplate;
//  
//  @LocalServerPort
//  private int serverPort;
//
//  protected String getBaseUri() {
//    return String.format("http://localhost:%d/jeeps", serverPort);
//  }
  
//    fail("Not yet implemented");
}