package com.ragnala.pos.ui.receipt

import com.ragnala.pos.data.db.OrderItemEntity

internal fun distinctDiscoveryProducts(items: List<OrderItemEntity>): List<String> =
    items.distinctBy { it.productId }.map { it.productName }

internal fun discoveryHeading(productCount: Int): String =
    if (productCount == 1) "Today's little discovery:" else "Today's little discoveries:"
