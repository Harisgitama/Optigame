package com.optigame.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.optigame.app.databinding.ItemAppBinding
import com.optigame.app.model.AppInfo

class AppAdapter(
    private val onItemClick: (AppInfo) -> Unit,
    private val onItemLongClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvPackageName.text = app.packageName
            binding.tvAppType.text = if (app.isSystemApp) "Sistem" else "User"

            Glide.with(binding.root.context)
                .load(app.icon)
                .into(binding.ivAppIcon)

            binding.root.setOnClickListener { onItemClick(app) }
            binding.root.setOnLongClickListener {
                onItemLongClick(app)
                true
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem == newItem
    }
}
