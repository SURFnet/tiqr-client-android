package org.tiqr.authenticator.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {

    @get:LayoutRes
    abstract val layout: Int

    protected lateinit var binding: T


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layout)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

}