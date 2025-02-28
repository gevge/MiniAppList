package net.gevge.miniapplist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.gevge.miniapplist.R
import net.gevge.miniapplist.data.AppLauncherData

class AppListAdapter(
    private val clickListener: ClickListener,
    private val iconCallback: IconLoadCallback
) :
    ListAdapter<AppLauncherData, AppListAdapter.AppInfoHolder>(DiffCallback()) {
    interface ClickListener {
        fun onClick(appLauncherData: AppLauncherData)
    }

    interface IconLoadCallback {
        fun onLoadIcon(appLauncherData: AppLauncherData)
    }

    class AppInfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var appLauncherData: AppLauncherData
        val imgIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        val txtAppName: TextView = itemView.findViewById(R.id.txtAppName)
        val txtPkgName: TextView = itemView.findViewById(R.id.txtPkgName)
    }

    class DiffCallback : DiffUtil.ItemCallback<AppLauncherData>() {
        override fun areItemsTheSame(oldItem: AppLauncherData, newItem: AppLauncherData): Boolean {
            return oldItem.launcherActivityInfo.componentName == newItem.launcherActivityInfo.componentName
        }

        override fun areContentsTheSame(oldItem: AppLauncherData, newItem: AppLauncherData): Boolean {
            return oldItem.lastUpdatedTime == newItem.lastUpdatedTime
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoHolder {
        return AppInfoHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_app_info, parent, false)
        ).apply {
            itemView.setOnClickListener {
                clickListener.onClick(appLauncherData)
            }
        }
    }

    override fun onBindViewHolder(holder: AppInfoHolder, position: Int) {
        val item = getItem(position)
        holder.appLauncherData = item
        holder.txtAppName.text = item.name
        val packageName = item.launcherActivityInfo.applicationInfo.packageName
        holder.txtPkgName.text = packageName
        if (item.icon != null) {
            item.isRefreshPending = false
            holder.imgIcon.setImageDrawable(item.icon)
        } else {
            item.isRefreshPending = true
            holder.imgIcon.setImageDrawable(null)
            iconCallback.onLoadIcon(item)
        }
        if (holder.imgIcon.visibility != View.VISIBLE) {
            holder.imgIcon.visibility = View.VISIBLE
        }
    }

    fun notifyItemChangedIfNoIcon(position: Int) {
        if (position < 0 || position >= itemCount) return
        if (getItem(position).isRefreshPending) {
            notifyItemChanged(position, 0)
        }
    }
}