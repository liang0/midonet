/*
 * Copyright (c) 2012. Midokura Japan K.K.
 */

package com.midokura.midonet.client.exception;

import com.sun.jersey.api.client.ClientResponse;

/**
 * User: tomoe
 * Date: 8/14/12
 * Time: 2:22 PM
 */


public class HttpBadRequestException extends HttpException {

    public HttpBadRequestException(ClientResponse response) {
        super(response);
    }
}