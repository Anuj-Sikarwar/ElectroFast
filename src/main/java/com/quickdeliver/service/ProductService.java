package com.quickdeliver.service;

import com.quickdeliver.dto.request.ProductRequest;
import com.quickdeliver.dto.response.ProductResponse;
import com.quickdeliver.entity.Category;
import com.quickdeliver.entity.Product;
import com.quickdeliver.entity.Shop;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.CategoryRepository;
import com.quickdeliver.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopService shopService;
    private final CategoryRepository categoryRepository;

    public ProductResponse add(Long shopId, ProductRequest request) {
        Shop shop = shopService.getShopOrThrow(shopId);

        Product product = new Product();
        mapRequestToProduct(request, product, shop);

        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = getProductOrThrow(productId);
        mapRequestToProduct(request, product, product.getShop());

        return ProductResponse.from(productRepository.save(product));
    }

    public void delete(Long productId) {
        Product product = getProductOrThrow(productId);
        product.setAvailable(false);
        productRepository.save(product);
    }

    public void updateStock(Long productId, int quantity) {
        if (quantity < 0) throw new BadRequestException("Stock cannot be negative");
        Product product = getProductOrThrow(productId);
        product.setStockQuantity(quantity);
        product.setAvailable(quantity > 0);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listByCity(Long cityId, Pageable pageable) {
        return productRepository.findByCityId(cityId, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchInCity(Long cityId, String query, Pageable pageable) {
        return productRepository.searchInCity(cityId, query, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listByCityAndCategory(Long cityId, Long categoryId, Pageable pageable) {
        return productRepository.findByCityIdAndCategoryId(cityId, categoryId, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return ProductResponse.from(getProductOrThrow(productId));
    }

    private void mapRequestToProduct(ProductRequest request, Product product, Shop shop) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setMrp(request.getMrp());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setStockQuantity(request.getStockQuantity());
        product.setAvailable(request.getStockQuantity() > 0);
        product.setShop(shop);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }
    }

    public Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }
}
