package com.kwanghoon.jpashop.controller;

import com.kwanghoon.jpashop.domain.Address;
import com.kwanghoon.jpashop.domain.Order;
import com.kwanghoon.jpashop.domain.OrderItem;
import com.kwanghoon.jpashop.domain.OrderStatus;
import com.kwanghoon.jpashop.repository.OrderRepository;
import com.kwanghoon.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    /*
    * V1
    * 엔티티를 직접 노출
    */
    @GetMapping("/api/v1/orders")
    public List<Order> orderV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        /* 프록시 강제 초기화 */
        for (Order order : all) {
            order.getMember().getAddress();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.forEach(o -> o.getItem().getName());
        }

        return all;
    }

    /*
    * V2
    * 엔티티를 DTO로 변환 (fetch join x)
    */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> orderV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        return orders
            .stream()
            .map(OrderDto::new)
            .collect(Collectors.toList());
    }

    /*
     * V3
     * 페치 조인을 사용 해서 성능 최적화
     * distinct를 사용하여 중복 row 제거 (1:N inner join 시 중복 row 발생)
     *
     * 단점
     * Collection을 페치 조인할 경우 페이징은 사용 불가능
     * 엄밀하게는 페이징 기능은 수행은 되지만 DB에서 수행하지 않고 어플리케이션 메모리로 수행
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders
            .stream()
            .map(OrderDto::new)
            .collect(Collectors.toList());
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            /* OrderItem 을 그대로 노출하는 것이 아닌 DTO로 변환해서 전달 */
            orderItems = order
                .getOrderItems()
                .stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());;

        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName; // 상품 명
        private int orderPrice; // 주문 가격
        private int count; // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}