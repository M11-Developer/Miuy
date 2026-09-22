package com.miyu.companion.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Minimal lifecycle owner for the floating overlay window.
 *
 * A WindowManager view (the companion bubble) is not attached to any Activity, so it has no
 * ViewTreeLifecycleOwner. Jetpack Compose requires one before ComposeView.setContent() will
 * run; without it the bubble throws IllegalStateException on show. This owner provides exactly
 * the three interfaces Compose needs and nothing more:
 *
 *  - LifecycleOwner       -> drives recomposition
 *  - SavedStateRegistryOwner -> satisfies ComposeView's contract
 *  - ViewModelStoreOwner  -> lets us clear any state when the bubble is removed
 *
 * Privacy: this class stores nothing, logs nothing and has no access to user data.
 */
class BubbleLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry.createUnsafe(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
