package com.quickdeliver.service;

import com.quickdeliver.dto.request.ShopRequest;
import com.quickdeliver.dto.response.ShopResponse;
import com.quickdeliver.entity.City;
import com.quickdeliver.entity.Shop;
import com.quickdeliver.entity.User;
import com.quickdeliver.enums.Role;
import com.quickdeliver.enums.ShopStatus;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.CityRepository;
import com.quickdeliver.repository.ShopRepository;
import com.quickdeliver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShopResponse register(ShopRequest request, String registeredByPhone) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));

        User member = userRepository.findByPhone(request.getManagerPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!(member.getRole() == Role.MEMBER || member.getRole() == Role.ADMIN)) {
            throw new IllegalArgumentException("User must be a MEMBER or ADMIN");
        }

        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());
        shop.setManagerPhone(request.getManagerPhone());
        shop.setManagerName(request.getManagerName());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setCity(city);
        shop.setRegisteredBy(member);
        shop.setStatus(ShopStatus.PENDING);

        return ShopResponse.from(shopRepository.save(shop));
    }


    @Transactional
    public ShopResponse update(Long shopId, ShopRequest request) {
        Shop shop = getShopOrThrow(shopId);
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());
        shop.setManagerPhone(request.getManagerPhone());
        shop.setManagerName(request.getManagerName());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setCity(city);

        return ShopResponse.from(shopRepository.save(shop));
    }

    @Transactional
    public void setStatus(Long shopId, ShopStatus status) {
        Shop shop = getShopOrThrow(shopId);
        shop.setStatus(status);
        shopRepository.save(shop);
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> listByCityAndStatus(Long cityId, ShopStatus status, Pageable pageable) {
        return shopRepository.findByCityIdAndStatus(cityId, status, pageable)
                .map(ShopResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> listAll(Pageable pageable) {
        return shopRepository.findAll(pageable).map(ShopResponse::from);
    }

    @Transactional(readOnly = true)
    public ShopResponse getById(Long shopId) {
        return ShopResponse.from(getShopOrThrow(shopId));
    }

    public Shop getShopOrThrow(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found: " + shopId));
    }
}
