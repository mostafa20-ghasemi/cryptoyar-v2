package ir.cryptoyar.app.billing

import android.content.Context

class MyketBillingRepository(private val context: Context) {
    fun purchaseProduct(sku: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onSuccess()
    }
    fun dispose() {}
}