package com.productorder.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.productorder.feign.IProductServiceFeignClient;
import com.productorder.model.Product;

import feign.FeignException;

@Service
public class OrderServiceImpl implements IOrderService{

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
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("Product with id "+productId+" not found");
		} catch (FeignException ex) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("Product service is currently unreachable, please try again later");
		}

		if(product!=null)
			return ResponseEntity.ok("Order placed successfully for "+product.getProductName());
		else
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("Product with id "+productId+" not found");
	}

	@Override
	public List<Product> viewAllProducts() {
		try {
			return feignClient.getAllProducts();
		} catch (FeignException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Product service is currently unreachable, please try again later");
		}
	}

}