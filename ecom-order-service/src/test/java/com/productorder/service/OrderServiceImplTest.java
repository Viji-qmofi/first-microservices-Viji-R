package com.productorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.productorder.feign.IProductServiceFeignClient;
import com.productorder.model.Product;

import feign.FeignException;
import feign.Request;
import feign.Response;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@Mock
	private IProductServiceFeignClient feignClient;

	@InjectMocks
	private OrderServiceImpl orderService;

	@Test
	void placeOrder_returnsConfirmation_whenProductExists() {
		Product product = new Product("Mobile", 1, "Samsung", "Electronics");
		when(feignClient.getById(1)).thenReturn(product);

		ResponseEntity<String> result = orderService.placeOrder(1);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("Order placed successfully for Mobile");
	}

	@Test
	void placeOrder_throwsNotFound_whenProductDoesNotExist() {
		when(feignClient.getById(999)).thenThrow(notFoundException(999));

		assertThatThrownBy(() -> orderService.placeOrder(999))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void placeOrder_throwsServiceUnavailable_whenProductServiceUnreachable() {
		when(feignClient.getById(1)).thenThrow(generalFeignException());

		assertThatThrownBy(() -> orderService.placeOrder(1))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	private FeignException notFoundException(int productId) {
		return errorStatus(404, "/catalog-service/v1/products/productId/" + productId);
	}

	private FeignException generalFeignException() {
		return errorStatus(500, "/catalog-service/v1/products/productId/1");
	}

	private FeignException errorStatus(int status, String url) {
		Request request = Request.create(Request.HttpMethod.GET, url,
				Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
		Response response = Response.builder()
				.status(status)
				.reason("error")
				.request(request)
				.headers(Collections.emptyMap())
				.build();
		return FeignException.errorStatus("IProductServiceFeignClient#getById(int)", response);
	}

}
