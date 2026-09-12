package com.productorder.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.productorder.feign.IProductServiceFeignClient;
import com.productorder.model.Product;

import feign.FeignException;
import feign.RetryableException;

@Service
public class OrderServiceImpl implements IOrderService{

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private IProductServiceFeignClient feignClient;

	@Override
	public ResponseEntity<String> placeOrder(int productId) {
		// check if the product by id is available if yes place order
		//	else cancel the order
		Product product;
		try {
			product = feignClient.getById(productId);
		} catch (FeignException.NotFound ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Product with id "+productId+" not found");
		// RetryableException is a subtype of FeignException thrown only when no HTTP response was received
		// at all (connection refused, timeout, DNS failure) -- the true "service unreachable" case. It is
		// caught before the broader FeignException below because it is a more specific subtype (general/broad
		// catches must come last in Java). Logged at WARN, not ERROR, since this is treated as an expected,
		// self-resolving operational condition rather than a bug, and maps to 503 (Product service is
		// currently unreachable).
		} catch (RetryableException ex) {
			logger.warn("IProductServiceFeignClient#getById unreachable for productId {}: {}",
					productId, ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Product service is currently unreachable, please try again later");
		// This branch only fires when a real HTTP response was received from the downstream service (not the
		// unreachable case above, and not the 404 NotFound case handled earlier) -- e.g. a genuine 4xx
		// (400/401) or 5xx from product-service. Logged at ERROR with the actual status code, since this may
		// indicate a real bug or contract violation, not a transient outage. Status is mapped by range:
		// 5xx -> 502 (Bad Gateway), other non-404 4xx -> 500, and the downstream response body/detail is
		// deliberately not echoed to the caller.
		} catch (FeignException ex) {
			logger.error("IProductServiceFeignClient#getById failed for productId {}, status {}: {}",
					productId, ex.status(), ex.getMessage());
			if (ex.status() >= 500 && ex.status() < 600) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Product service returned an error, please try again later");
			}
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unable to process request due to an internal error");
		}

		return ResponseEntity.ok("Order placed successfully for "+product.getProductName());
	}

	@Override
	public List<Product> viewAllProducts() {
		try {
			return feignClient.getAllProducts();
		// Same RetryableException-before-FeignException distinction as placeOrder() above; see the comments
		// there for the full explanation (no NotFound case here since this call returns a list, not a single
		// resource).
		} catch (RetryableException ex) {
			logger.warn("IProductServiceFeignClient#getAllProducts unreachable: {}", ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Product service is currently unreachable, please try again later");
		} catch (FeignException ex) {
			logger.error("IProductServiceFeignClient#getAllProducts failed, status {}: {}",
					ex.status(), ex.getMessage());
			if (ex.status() >= 500 && ex.status() < 600) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Product service returned an error, please try again later");
			}
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unable to process request due to an internal error");
		}
	}

}
