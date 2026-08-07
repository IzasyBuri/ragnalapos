package com.ragnala.pos.ui

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ragnala.pos.data.db.RagnalaDatabase
import com.ragnala.pos.data.repo.CatalogRepository
import com.ragnala.pos.data.repo.ExpenseRepository
import com.ragnala.pos.data.repo.InventoryRepository
import com.ragnala.pos.service.AuditService
import com.ragnala.pos.service.BackupService
import com.ragnala.pos.service.OrderService
import com.ragnala.pos.service.PinService
import com.ragnala.pos.service.ReportsService
import com.ragnala.pos.service.SettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ragnala.pos.ui.customer.BrowseScreen
import com.ragnala.pos.ui.customer.BrowseViewModel
import com.ragnala.pos.ui.customer.CartScreen
import com.ragnala.pos.ui.customer.CartViewModel
import com.ragnala.pos.ui.customer.NameScreen
import com.ragnala.pos.ui.customer.OrderConfirmScreen
import com.ragnala.pos.ui.customer.OrderConfirmViewModel
import com.ragnala.pos.ui.customer.OrderThankYouScreen
import com.ragnala.pos.ui.customer.ProductDetailScreen
import com.ragnala.pos.ui.customer.ProductDetailViewModel
import com.ragnala.pos.ui.barista.BaristaQueueScreen
import com.ragnala.pos.ui.barista.BaristaQueueViewModel
import com.ragnala.pos.ui.barista.BaristaUnlockScreen
import com.ragnala.pos.ui.barista.BaristaUnlockViewModel
import com.ragnala.pos.ui.barista.BaristaDetailScreen
import com.ragnala.pos.ui.barista.BaristaDetailViewModel
import com.ragnala.pos.ui.barista.BaristaDetailViewModelFactory
import com.ragnala.pos.ui.management.ManagementScreen
import com.ragnala.pos.ui.management.ManagementViewModel
import com.ragnala.pos.ui.management.ManagementViewModelFactory
import com.ragnala.pos.ui.management.ProductEditorScreen
import com.ragnala.pos.ui.management.ProductEditorViewModel
import com.ragnala.pos.ui.management.ProductListScreen
import com.ragnala.pos.ui.management.ProductListViewModel
import com.ragnala.pos.ui.management.InventoryScreen
import com.ragnala.pos.ui.management.InventoryViewModel
import com.ragnala.pos.ui.management.ExpenseScreen
import com.ragnala.pos.ui.management.ExpenseViewModel
import com.ragnala.pos.ui.management.ReportsScreen
import com.ragnala.pos.ui.management.ReportsViewModel
import com.ragnala.pos.ui.management.BackupScreen
import com.ragnala.pos.ui.management.BackupViewModel

// DESIGN.md Â§Navigation â€” simple, predictable, no deep nesting (max depth 3).
// Two top-level modes; internal flows stay shallow.

object RagnalaRoutes {
    const val CUSTOMER = "customer"
    const val BARISTA = "barista"
    const val BARISTA_DETAIL = "barista/{orderId}"
    const val MANAGEMENT = "management"
    const val PRODUCTS = "management/products"
    const val PRODUCT_NEW = "management/products/new"
    const val PRODUCT_EDIT = "management/products/edit/{productId}"
    const val INVENTORY = "management/inventory"
    const val EXPENSES = "management/expenses"
    const val REPORTS = "management/reports"
    const val BACKUP = "management/backup"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART = "cart"
    const val NAME = "name"
    const val CONFIRM = "confirm"
    const val THANK_YOU = "thank_you"
    fun productDetail(productId: String) = "product/$productId"
    fun productEdit(productId: String) = "management/products/edit/$productId"
    fun baristaDetail(orderId: String) = "barista/$orderId"
}

/** App-level composition root â€” no DI framework (PRD Â§15). */
object AppGraph {
    @Volatile
    private var database: RagnalaDatabase? = null

    fun database(context: android.content.Context): RagnalaDatabase =
        database ?: synchronized(this) {
            database ?: RagnalaDatabase.get(context.applicationContext).also { database = it }
        }

    fun catalogRepository(context: android.content.Context): CatalogRepository =
        CatalogRepository(
            categoryDao = database(context).categoryDao(),
            productDao = database(context).productDao(),
            modifierDao = database(context).modifierDao(),
            ingredientDao = database(context).ingredientDao(),
        )

    fun orderService(context: android.content.Context): OrderService = OrderService(
        db = database(context),
        orderDao = database(context).orderDao(),
        ingredientDao = database(context).ingredientDao(),
        auditService = auditService(context),
    )

    fun orderDao(context: android.content.Context) = database(context).orderDao()

    fun paymentDao(context: android.content.Context) = database(context).paymentDao()

    fun pinService(context: android.content.Context): PinService = PinService(
        settingsDao = database(context).settingsDao(),
        auditDao = database(context).auditDao(),
    )

    fun settingsService(context: android.content.Context): SettingsService = SettingsService(
        dao = database(context).settingsDao(),
    )

    fun auditService(context: android.content.Context): AuditService = AuditService(
        auditDao = database(context).auditDao(),
    )

    fun inventoryRepository(context: android.content.Context): InventoryRepository =
        InventoryRepository(
            ingredientDao = database(context).ingredientDao(),
            auditDao = database(context).auditDao(),
        )

    fun expenseRepository(context: android.content.Context): ExpenseRepository =
        ExpenseRepository(expenseDao = database(context).expenseDao())

    fun reportsService(context: android.content.Context): ReportsService = ReportsService(
        orderDao = database(context).orderDao(),
        paymentDao = database(context).paymentDao(),
        expenseRepository = expenseRepository(context),
    )

    fun backupService(context: android.content.Context): BackupService =
        BackupService(context.applicationContext, database(context.applicationContext))
}

@Composable
fun RagnalaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val cartViewModel: CartViewModel = viewModel()
    val context = LocalContext.current
    val scPercent = remember { mutableStateOf(0.05) }
    val taxPercent = remember { mutableStateOf(0.11) }

    // Barista Mode session unlock (PRD Â§9): unlocked once per app session.
    // rememberSaveable so rotation does not silently re-lock the queue. Payment still
    // requires a fresh PIN entry in the detail screen (defense in depth).
    val baristaUnlocked = rememberSaveable { mutableStateOf(false) }

    // Load settings on IO thread â€” SettingsService returns percentage points (5.0 = 5%),
    // which is what Pricing.calculate expects (it divides by 100 internally).
    LaunchedEffect(Unit) {
        val settings = AppGraph.settingsService(context)
        scPercent.value = withContext(Dispatchers.IO) { settings.serviceChargePercent() }
        taxPercent.value = withContext(Dispatchers.IO) { settings.taxPercent() }

        // Startup order recovery (PRD Â§9): surface orphaned WAITING_PAYMENT orders that have
        // sat idle beyond the recovery window (they still appear in the barista queue for the
        // barista to recover or cancel) and purge abandoned drafts older than the idle window.
        withContext(Dispatchers.IO) {
            val orderService = AppGraph.orderService(context)
            val now = System.currentTimeMillis()
            val stale = orderService.staleWaitingPayment(
                now - settings.recoveryWindowMinutes() * 60_000L,
            )
            if (stale.isNotEmpty()) {
                AppGraph.auditService(context).record(
                    action = "RECOVERY_FLAG",
                    entityType = "order",
                    entityId = stale.joinToString(",") { it.id },
                    delta = "${stale.size} stale WAITING_PAYMENT order(s) awaiting recovery/cancel",
                    userLabel = "system",
                    reason = "Startup recovery scan (PRD Â§9)",
                    now = now,
                )
            }
            orderService.purgeAbandonedDrafts(
                olderThan = now - settings.idleTimeoutMinutes() * 60_000L,
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    selected = currentRoute == RagnalaRoutes.CUSTOMER,
                    onClick = {
                        navController.navigate(RagnalaRoutes.CUSTOMER) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Outlined.Coffee, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_menu)) },
                )
                NavigationBarItem(
                    selected = currentRoute == RagnalaRoutes.BARISTA,
                    onClick = {
                        navController.navigate(RagnalaRoutes.BARISTA) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_barista)) },
                )
                NavigationBarItem(
                    selected = currentRoute == RagnalaRoutes.MANAGEMENT ||
                        currentRoute == RagnalaRoutes.PRODUCTS ||
                        currentRoute == RagnalaRoutes.PRODUCT_NEW,
                    onClick = {
                        navController.navigate(RagnalaRoutes.MANAGEMENT) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_manage)) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = RagnalaRoutes.CUSTOMER,
            modifier = Modifier.padding(padding),
        ) {
            composable(RagnalaRoutes.CUSTOMER) {
                val context = LocalContext.current
                val vm: BrowseViewModel = viewModel(
                    factory = BrowseViewModel.Factory(AppGraph.catalogRepository(context)),
                )
                val cartItems = cartViewModel.items.collectAsState().value
                val baseProductQuantities = cartItems
                    .filter { it.modifiers.isEmpty() }
                    .groupBy { it.productId }
                    .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
                BrowseScreen(
                    categories = vm.categories.collectAsState().value,
                    products = vm.products.collectAsState().value,
                    selectedCategoryId = vm.selectedCategory.collectAsState().value,
                    quickAddEligibleProductIds = vm.quickAddEligibleProductIds.collectAsState().value,
                    baseProductQuantities = baseProductQuantities,
                    onCategorySelected = vm::selectCategory,
                    onProductClick = { product ->
                        navController.navigate(RagnalaRoutes.productDetail(product.id))
                    },
                    onQuickAdd = { product ->
                        cartViewModel.add(
                            productId = product.id,
                            productName = product.name,
                            unitPrice = product.price,
                            quantity = 1,
                            imagePath = product.imagePath,
                        )
                    },
                    onQuickRemove = { product ->
                        cartViewModel.decrementBaseProduct(product.id)
                    },
                    onCartClick = { navController.navigate(RagnalaRoutes.CART) },
                    cartCount = cartViewModel.itemCount.collectAsState().value,
                )
            }
            composable(
                route = RagnalaRoutes.PRODUCT_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                val context = LocalContext.current
                val detailVm: ProductDetailViewModel = viewModel(
                    factory = ProductDetailViewModel.Factory(
                        AppGraph.catalogRepository(context),
                        productId,
                    ),
                )
                ProductDetailScreen(
                    product = detailVm.product.collectAsState().value,
                    groups = detailVm.groups.collectAsState().value,
                    optionsByGroup = detailVm.optionsByGroup.collectAsState().value,
                    onAddToCart = { quantity, modifiers ->
                        val p = detailVm.product.value ?: return@ProductDetailScreen
                        cartViewModel.add(
                            productId = p.id,
                            productName = p.name,
                            unitPrice = p.price + modifiers.sumOf { it.priceDelta },
                            quantity = quantity,
                            modifiers = modifiers,
                            imagePath = p.imagePath,
                        )
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(RagnalaRoutes.CART) {
                val items = cartViewModel.items.collectAsState().value
                CartScreen(
                    items = items,
                    subtotal = cartViewModel.subtotal.collectAsState().value,
                    onQuantityChange = { index, quantity ->
                        cartViewModel.setQuantity(index, quantity)
                    },
                    onRemove = { index -> cartViewModel.removeAt(index) },
                    onContinue = { navController.navigate(RagnalaRoutes.NAME) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(RagnalaRoutes.NAME) {
                val items = cartViewModel.items.collectAsState().value
                val subtotal = cartViewModel.subtotal.collectAsState().value
                // Create confirm VM for this flow
                val confirmVm: OrderConfirmViewModel = viewModel(
                    factory = OrderConfirmViewModel.Factory(
                        orderService = AppGraph.orderService(context),
                        cartItems = items,
                        customerName = "", // will be updated onContinue
                        scPercent = scPercent.value,
                        taxPercent = taxPercent.value,
                    ),
                )
                NameScreen(
                    onContinue = { name ->
                        // Audit M5: URL-encode the customer name so spaces/special chars can't
                        // corrupt the nav route. Navigation decodes it back on read.
                        val encoded = android.net.Uri.encode(name.trim())
                        navController.navigate("${RagnalaRoutes.CONFIRM}?name=$encoded")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${RagnalaRoutes.CONFIRM}?name={name}",
                arguments = listOf(navArgument("name") { type = NavType.StringType }),
            ) { entry ->
                val name = entry.arguments?.getString("name").orEmpty()
                val items = cartViewModel.items.collectAsState().value
                val confirmVm: OrderConfirmViewModel = viewModel(
                    factory = OrderConfirmViewModel.Factory(
                        orderService = AppGraph.orderService(context),
                        cartItems = items,
                        customerName = name,
                        scPercent = scPercent.value,
                        taxPercent = taxPercent.value,
                    ),
                )
                val result = confirmVm.result.collectAsState().value
                LaunchedEffect(result) {
                    if (result is OrderConfirmViewModel.Result.Success) {
                        cartViewModel.clear()
                        navController.navigate("${RagnalaRoutes.THANK_YOU}?orderId=${result.orderId}") {
                            popUpTo(RagnalaRoutes.CUSTOMER) { inclusive = true }
                        }
                    }
                }
                OrderConfirmScreen(
                    items = items,
                    customerName = name,
                    scPercent = scPercent.value,
                    taxPercent = taxPercent.value,
                    onBack = { navController.popBackStack() },
                    onConfirm = {
                        val now = System.currentTimeMillis()
                        confirmVm.confirmOrder(now)
                    },
                    result = result,
                    submitting = confirmVm.submitting.collectAsState().value,
                )
            }
            composable(
                route = "${RagnalaRoutes.THANK_YOU}?orderId={orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                OrderThankYouScreen(
                    orderId = orderId,
                    onBackToMenu = {
                        navController.navigate(RagnalaRoutes.CUSTOMER) {
                            popUpTo(RagnalaRoutes.CUSTOMER) { inclusive = true }
                        }
                    },
                )
            }
            composable(RagnalaRoutes.BARISTA) {
                val settingsSvc = AppGraph.settingsService(context)
                val pinSvc = AppGraph.pinService(context)
                // Determine if PIN is disabled for today (owner decision)
                val pinDisabledToday by produceState(initialValue = false) {
                    val today = java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.US,
                    ).format(java.util.Calendar.getInstance().time)
                    value = withContext(Dispatchers.IO) {
                        settingsSvc.baristaPinDisabledDate() == today
                    }
                }
                if (baristaUnlocked.value || pinDisabledToday) {
                    val baristaVm: BaristaQueueViewModel = viewModel(
                        factory = BaristaQueueViewModel.Factory(AppGraph.orderDao(context)),
                    )
                    BaristaQueueScreen(
                        viewModel = baristaVm,
                        onOrderClick = { order -> navController.navigate(RagnalaRoutes.baristaDetail(order.id)) },
                        onManageMenu = { navController.navigate(RagnalaRoutes.PRODUCTS) },
                    )
                } else {
                    val unlockVm: BaristaUnlockViewModel = viewModel(
                        factory = BaristaUnlockViewModel.Factory(pinSvc, settingsSvc),
                    )
                    BaristaUnlockScreen(
                        viewModel = unlockVm,
                        onUnlocked = { baristaUnlocked.value = true },
                    )
                }
            }
            composable(RagnalaRoutes.BARISTA_DETAIL) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val detailVm: BaristaDetailViewModel = viewModel(
                    factory = BaristaDetailViewModelFactory(
                        orderId = orderId,
                        orderDao = AppGraph.orderDao(context),
                        paymentDao = AppGraph.paymentDao(context),
                        orderService = AppGraph.orderService(context),
                        settingsService = AppGraph.settingsService(context),
                    ),
                )
                BaristaDetailScreen(viewModel = detailVm, onBack = { navController.popBackStack() })
            }
            composable(RagnalaRoutes.MANAGEMENT) {
                val mgmtVm: ManagementViewModel = viewModel(
                    factory = ManagementViewModelFactory(
                        settingsService = AppGraph.settingsService(context),
                        pinService = AppGraph.pinService(context),
                    ),
                )
                ManagementScreen(
                    viewModel = mgmtVm,
                    onBack = { navController.popBackStack() },
                    onProductsClick = { navController.navigate(RagnalaRoutes.PRODUCTS) },
                    onInventoryClick = { navController.navigate(RagnalaRoutes.INVENTORY) },
                    onExpensesClick = { navController.navigate(RagnalaRoutes.EXPENSES) },
                    onReportsClick = { navController.navigate(RagnalaRoutes.REPORTS) },
                    onBackupClick = { navController.navigate(RagnalaRoutes.BACKUP) },
                )
            }
            composable(RagnalaRoutes.INVENTORY) {
                val inventoryVm: InventoryViewModel = viewModel(
                    factory = InventoryViewModel.Factory(AppGraph.inventoryRepository(context)),
                )
                InventoryScreen(viewModel = inventoryVm, onBack = { navController.popBackStack() })
            }
            composable(RagnalaRoutes.EXPENSES) {
                val expenseVm: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModel.Factory(AppGraph.expenseRepository(context)),
                )
                ExpenseScreen(viewModel = expenseVm, onBack = { navController.popBackStack() })
            }
            composable(RagnalaRoutes.REPORTS) {
                val reportsVm: ReportsViewModel = viewModel(
                    factory = ReportsViewModel.Factory(AppGraph.reportsService(context)),
                )
                ReportsScreen(viewModel = reportsVm, onBack = { navController.popBackStack() })
            }
            composable(RagnalaRoutes.BACKUP) {
                val backupVm: BackupViewModel = viewModel(
                    factory = BackupViewModel.Factory(AppGraph.backupService(context)),
                )
                BackupScreen(viewModel = backupVm, onBack = { navController.popBackStack() })
            }
            composable(RagnalaRoutes.PRODUCTS) {
                val productListVm: ProductListViewModel = viewModel(
                    factory = ProductListViewModel.Factory(AppGraph.catalogRepository(context)),
                )
                ProductListScreen(
                    rows = productListVm.rows.collectAsState().value,
                    onBack = { navController.popBackStack() },
                    onAddProduct = { navController.navigate(RagnalaRoutes.PRODUCT_NEW) },
                    onProductClick = { productId ->
                        navController.navigate(RagnalaRoutes.productEdit(productId))
                    },
                    onToggleAvailability = { productId, currentlyAvailable ->
                        productListVm.toggleAvailability(productId, currentlyAvailable)
                    },
                )
            }
            composable(RagnalaRoutes.PRODUCT_NEW) {
                val editorVm: ProductEditorViewModel = viewModel(
                    factory = ProductEditorViewModel.Factory(AppGraph.catalogRepository(context)),
                )
                val editorState by editorVm.state.collectAsState()
                val categories by editorVm.categories.collectAsState()
                val groups by editorVm.allGroups.collectAsState()
                ProductEditorScreen(
                    state = editorState,
                    categories = categories,
                    groups = groups,
                    onNameChange = editorVm::setName,
                    onDescriptionChange = editorVm::setDescription,
                    onPriceChange = editorVm::setPrice,
                    onCategoryChange = editorVm::setCategory,
                    onAvailableChange = editorVm::setAvailable,
                    onImagePathChange = editorVm::setImagePath,
                    onToggleGroup = editorVm::toggleGroup,
                    onAddRecipeRow = editorVm::addRecipeRow,
                    onRemoveRecipeRow = editorVm::removeRecipeRow,
                    onRecipeIngredientChange = editorVm::setRecipeIngredient,
                    onRecipeQuantityChange = editorVm::setRecipeQuantity,
                    ingredients = editorVm.ingredients.collectAsState().value,
                    onSave = editorVm::save,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = RagnalaRoutes.PRODUCT_EDIT,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                val editorVm: ProductEditorViewModel = viewModel(
                    factory = ProductEditorViewModel.Factory(AppGraph.catalogRepository(context)),
                )
                val editorState by editorVm.state.collectAsState()
                val categories by editorVm.categories.collectAsState()
                val groups by editorVm.allGroups.collectAsState()
                LaunchedEffect(productId) { editorVm.loadForEdit(productId) }
                ProductEditorScreen(
                    state = editorState,
                    categories = categories,
                    groups = groups,
                    onNameChange = editorVm::setName,
                    onDescriptionChange = editorVm::setDescription,
                    onPriceChange = editorVm::setPrice,
                    onCategoryChange = editorVm::setCategory,
                    onAvailableChange = editorVm::setAvailable,
                    onImagePathChange = editorVm::setImagePath,
                    onToggleGroup = editorVm::toggleGroup,
                    onAddRecipeRow = editorVm::addRecipeRow,
                    onRemoveRecipeRow = editorVm::removeRecipeRow,
                    onRecipeIngredientChange = editorVm::setRecipeIngredient,
                    onRecipeQuantityChange = editorVm::setRecipeQuantity,
                    ingredients = editorVm.ingredients.collectAsState().value,
                    onSave = editorVm::save,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
