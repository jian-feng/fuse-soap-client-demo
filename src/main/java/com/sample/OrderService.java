package com.sample;

import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * OrderService
 */
@WebService
public interface OrderService {

  String order(
    @WebParam(name = "product") String product, 
    @WebParam(name = "amount") int amount
    );

}