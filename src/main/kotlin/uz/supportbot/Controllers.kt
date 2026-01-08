package uz.supportbot

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/operator")
class OperatorAdminController(
    private val operatorService: OperatorService
) {


    @PostMapping("/create")
    fun createOperator(@RequestBody request: OperatorCreateRequest):OperatorResponse {
        return operatorService.createOperator(request)
    }


    @PostMapping("/delete")
    fun deleteOperator(@RequestBody request: OperatorCreateRequest): OperatorResponse {
        return operatorService.deleteOperator(request)
    }

    @GetMapping("/stats")
    fun getAllOperatorStats(): AllOperatorStatsResponse {
        return operatorService.getAllOperatorStats()
    }
}

