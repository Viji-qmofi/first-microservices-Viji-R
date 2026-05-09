package com.productorder.service;

import java.util.List;

import com.productorder.model.Product;

public interface IOrderService {
	
	String placeOrder(int productId);
	List<Product> viewAllProducts();

}
