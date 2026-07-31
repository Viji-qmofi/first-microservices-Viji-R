package com.productorder.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.productorder.model.Product;

public interface IOrderService {

	ResponseEntity<String> placeOrder(int productId);
	List<Product> viewAllProducts();

}
