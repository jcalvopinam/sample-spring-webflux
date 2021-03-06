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

package com.jcalvopinam.webflux;

import com.jcalvopinam.webflux.model.Product;
import com.jcalvopinam.webflux.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

/**
 * @author juan.calvopina
 */
@SpringBootApplication
@Slf4j
public class SampleSpringWebfluxApplication implements CommandLineRunner {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final ProductRepository productRepository;

    public SampleSpringWebfluxApplication(final ReactiveMongoTemplate reactiveMongoTemplate,
                                          final ProductRepository productRepository) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.productRepository = productRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleSpringWebfluxApplication.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {
        reactiveMongoTemplate.dropCollection("product")
                             .subscribe();

        Flux.just(new Product("MacBook Air", 999.00),
                  new Product("MacBook Pro", 1299.00),
                  new Product("iMac", 1499.00),
                  new Product("iMac Pro", 4999.00))
            .flatMap(productRepository::save)
            .subscribe(product -> log.info("Insert: {} {}", product.getId(), product.getName()));
    }

}
