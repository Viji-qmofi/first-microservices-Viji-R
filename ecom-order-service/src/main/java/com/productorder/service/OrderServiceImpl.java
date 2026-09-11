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
		} catch (RetryableException ex) {
			logger.warn("IProductServiceFeignClient#getById unreachable for productId {}: {}",
					productId, ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Product service is currently unreachable, please try again later");
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
