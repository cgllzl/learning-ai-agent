package com.enterprise.agent.agent;

import com.enterprise.agent.chat.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class OrderAgentController {

    private final OrderAgentService orderAgentService;

    public OrderAgentController(OrderAgentService orderAgentService) {
        this.orderAgentService = orderAgentService;
    }

    @PostMapping("/order")
    public ChatResponse order(@Valid @RequestBody OrderAgentRequest request) {
        return new ChatResponse(orderAgentService.chat(request.message()));
    }
}