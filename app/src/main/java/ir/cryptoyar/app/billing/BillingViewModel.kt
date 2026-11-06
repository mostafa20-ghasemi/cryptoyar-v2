package ir.cryptoyar.app.billing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingViewModel(private val repository: MyketBillingRepository) : ViewModel() {
    private val _purchasedSkus = MutableStateFlow<Set<String>>(emptySet())
    val purchasedSkus: StateFlow<Set<String>> = _purchasedSkus

    fun hasAccess(sku: String): Boolean {
        return _purchasedSkus.value.contains(sku)
    }

    fun buyAnalysis(sku: String) {
        repository.purchaseProduct(
            sku = sku,
            onSuccess = {
                _purchasedSkus.value = _purchasedSkus.value + sku
            },
            onError = { error -> println("Error: $error") }
        )
    }
}