/*
 * MIT License
 *
 * Copyright (c) 2021 JUAN CALVOPINA M
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jcalvopinam.webflux.handler;

import com.jcalvopinam.webflux.model.Product;
import com.jcalvopinam.webflux.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.BiFunction;

import static com.jcalvopinam.webflux.config.RouterFunctionConfig.API_V1_PRODUCTS;

/**
 * @author juan.calvopina
 */
@Component
@Slf4j
public class ProductHandler {

    private final ProductService productService;
    private final Validator validator;

    public ProductHandler(final ProductService productService,
                          final @Qualifier("webFluxValidator") Validator validator) {
        this.productService = productService;
        this.validator = validator;
    }

    public Mono<ServerResponse> findAll(final ServerRequest request) {
        log.info("Finding all {}", request.path());

        return ServerResponse.ok()
                             .body(productService.findAll(), Product.class);
    }

    public Mono<ServerResponse> findById(final ServerRequest request) {
        final String id = request.pathVariable("id");
        log.info("Finding by id: {} ", id);

        return productService.findById(id)
                             .flatMap(product -> ServerResponse.ok()
                                                               .body(BodyInserters.fromValue(product)))
                             .switchIfEmpty(ServerResponse.notFound()
                                                          .build());
    }

    public Mono<ServerResponse> createProduct(final ServerRequest request) {
        log.info("Creating a new product");

        return request.bodyToMono(Product.class)
                      .flatMap(product -> {
                          final Errors errors = new BeanPropertyBindingResult(product, Product.class.getName());
                          validator.validate(product, errors);

                          if (errors.hasErrors()) {
                              return Flux.fromIterable(errors.getFieldErrors())
                                         .map(productService::getFieldErrorMessage)
                                         .collectList()
                                         .flatMap(list -> ServerResponse.badRequest()
                                                                        .body(BodyInserters.fromValue(list)));
                          }
                          return productService.save(product)
                                               .flatMap(savedProduct -> ServerResponse
                                                       .created(URI.create(API_V1_PRODUCTS.concat("/")
                                                                                          .concat(savedProduct.getId())))
                                                       .body(BodyInserters.fromValue(savedProduct)));
                      });
    }

    public Mono<ServerResponse> updateProduct(final ServerRequest request) {
        final String id = request.pathVariable("id");
        log.info("Updating the product: {}", id);

        final Mono<Product> product = request.bodyToMono(Product.class);
        final Mono<Product> productDb = productService.findById(id);

        return productDb.zipWith(product, getMergeProduct())
                        .flatMap(mergedProduct -> ServerResponse.ok()
                                                                .body(productService.save(mergedProduct),
                                                                      Product.class))
                        .switchIfEmpty(ServerResponse.notFound()
                                                     .build());
    }

    public Mono<ServerResponse> deleteProduct(final ServerRequest request) {
        final String id = request.pathVariable("id");
        log.info("Deleting the product: {}", id);

        return productService.findById(id)
                             .flatMap(product -> productService.delete(product)
                                                               .then(ServerResponse.noContent()
                                                                                   .build()))
                             .switchIfEmpty(ServerResponse.notFound()
                                                          .build());
    }

    private BiFunction<Product, Product, Product> getMergeProduct() {
        return (foundProduct, incomingProduct) -> {
            foundProduct.setName(incomingProduct.getName());
            foundProduct.setPrice(incomingProduct.getPrice());
            return foundProduct;
        };
    }

}
