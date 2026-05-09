package com.productorder.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.productorder.feign.IProductServiceFeignClient;
import com.productorder.model.Product;

@Service
public class OrderServiceImpl implements IOrderService{
	
	@Autowired
	private IProductServiceFeignClient feignClient;

	@Override
	public String placeOrder(int productId) {
		// check if the product by id is available if yes place order
		//	else cancel the order
		Product product = feignClient.getById(productId);
		if(product!=null)
			return "Order placed successfully for "+product.getProductName();
		else
			return "Product with this id not available";
	}

	@Override
	public List<Product> viewAllProducts() {
	  return feignClient.getAllProducts();
	}

}