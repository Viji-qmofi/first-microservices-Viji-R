package com.productorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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
import feign.RetryableException;

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
	void placeOrder_throwsBadGateway_whenProductServiceReturns500() {
		when(feignClient.getById(1)).thenThrow(generalFeignException());

		assertThatThrownBy(() -> orderService.placeOrder(1))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void placeOrder_throwsServiceUnavailable_whenProductServiceUnreachable() {
		when(feignClient.getById(1))
				.thenThrow(unreachableException("/catalog-service/v1/products/productId/1"));

		assertThatThrownBy(() -> orderService.placeOrder(1))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void placeOrder_throwsInternalServerError_whenProductServiceReturnsOther4xx() {
		when(feignClient.getById(1)).thenThrow(badRequestException());

		assertThatThrownBy(() -> orderService.placeOrder(1))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void viewAllProducts_returnsList_whenProductServiceSucceeds() {
		List<Product> products = List.of(new Product("Mobile", 1, "Samsung", "Electronics"));
		when(feignClient.getAllProducts()).thenReturn(products);

		List<Product> result = orderService.viewAllProducts();

		assertThat(result).isEqualTo(products);
	}

	@Test
	void viewAllProducts_throwsServiceUnavailable_whenProductServiceUnreachable() {
		when(feignClient.getAllProducts())
				.thenThrow(unreachableException("/catalog-service/v1/products"));

		assertThatThrownBy(() -> orderService.viewAllProducts())
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void viewAllProducts_throwsBadGateway_whenProductServiceReturns500() {
		when(feignClient.getAllProducts()).thenThrow(errorStatusForGetAllProducts(500));

		assertThatThrownBy(() -> orderService.viewAllProducts())
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void viewAllProducts_throwsInternalServerError_whenProductServiceReturnsOther4xx() {
		when(feignClient.getAllProducts()).thenThrow(errorStatusForGetAllProducts(400));

		assertThatThrownBy(() -> orderService.viewAllProducts())
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private FeignException notFoundException(int productId) {
		return errorStatus(404, "/catalog-service/v1/products/productId/" + productId);
	}

	private FeignException generalFeignException() {
		return errorStatus(500, "/catalog-service/v1/products/productId/1");
	}

	private FeignException badRequestException() {
		return errorStatus(400, "/catalog-service/v1/products/productId/1");
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

	private FeignException errorStatusForGetAllProducts(int status) {
		Request request = Request.create(Request.HttpMethod.GET, "/catalog-service/v1/products",
				Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
		Response response = Response.builder()
				.status(status)
				.reason("error")
				.request(request)
				.headers(Collections.emptyMap())
				.build();
		return FeignException.errorStatus("IProductServiceFeignClient#getAllProducts()", response);
	}

	private RetryableException unreachableException(String url) {
		Request request = Request.create(Request.HttpMethod.GET, url,
				Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
		return new RetryableException(-1, "connection timed out", Request.HttpMethod.GET, (Long) null, request);
	}

}
