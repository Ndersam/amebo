package com.amebo.amebo.screens.topiclist.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amebo.amebo.R
import com.amebo.amebo.common.asTheme
import com.amebo.amebo.common.extensions.context
import com.amebo.amebo.databinding.ItemBoardTopicBinding
import com.amebo.amebo.databinding.ItemFeaturedTopicBinding
import com.amebo.amebo.databinding.ItemFooterBinding
import com.amebo.amebo.databinding.ItemSpecialTopicBinding
import com.amebo.amebo.screens.topiclist.main.SwipeToUnFollowCallback
import com.amebo.core.domain.*
import com.google.firebase.crashlytics.FirebaseCrashlytics

class ItemAdapter(
    private var listener: TopicListAdapterListener,
    private val topicList: TopicList
) : ListAdapter<Item, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    var page: BaseTopicListDataPage? = null
        set(value) {
            field = value
            if (value != null) {
                val list = mutableListOf<Item>()
                list.addAll(value.data.map {
                    when (topicList) {
                        is Featured -> FeaturedTopicItem(it)
                        is Board -> BoardTopicItem(it)
                        else -> OtherTopicItem(it)
                    }
                })
                list.add(FooterItem)
                submitList(list)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_BOARD_TOPIC -> BoardTopicItemVH(
                ItemBoardTopicBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
            ITEM_OTHER_TOPIC -> OtherTopicItemVH(
                ItemSpecialTopicBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
            ITEM_FEATURED_TOPIC -> FeaturedTopicItemVH(
                ItemFeaturedTopicBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
            ITEM_FOOTER -> FooterVH(ItemFooterBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException()
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is BoardTopicItem -> {
                val vh = holder as BoardTopicItemVH
                vh.bind(item.topic, listener)
            }
            is OtherTopicItem -> {
                val vh = holder as OtherTopicItemVH
                vh.bind(item.topic, listener)
            }
            is FeaturedTopicItem -> {
                val vh = holder as FeaturedTopicItemVH
                vh.bind(item.topic, listener)
            }
            is FooterItem -> {
                val vh = holder as FooterVH
                vh.bind(listener)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is BoardTopicItem -> ITEM_BOARD_TOPIC
        is OtherTopicItem -> ITEM_OTHER_TOPIC
        is FeaturedTopicItem -> ITEM_FEATURED_TOPIC
        is FooterItem -> ITEM_FOOTER
    }


    fun clear() {
        submitList(emptyList())
    }

    fun isEmpty() = itemCount == 0

    fun removeTopicAt(position: Int): Topic? {
        val copy = currentList.toMutableList()
        val item = copy.removeAt(position)
        submitList(copy)
        return when (item) {
            is BoardTopicItem -> item.topic
            is FeaturedTopicItem -> item.topic
            is OtherTopicItem -> item.topic
            else -> null
        }
    }

    class BoardTopicItemVH(private val binding: ItemBoardTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(topic: Topic, listener: TopicListAdapterListener) {
            binding.storyAuthor.text = topic.author!!.name
            binding.storyAuthor.setOnClickListener { listener.onAuthorClicked(topic.author!!) }
            binding.hasNewPostsIndicator.isVisible = topic.hasNewPosts
            binding.txtPostsCount.text = topic.postCount.toString()
            binding.storyTitle.text = topic.title
            binding.root.setOnClickListener { listener.onTopicClicked(topic) }

            val viewed = listener.hasViewedTopic(topic)
            markPrimaryAsRead(binding.storyTitle, viewed)
            markSecondaryAsRead(binding.storyAuthor, viewed)
            markSecondaryAsRead(binding.txtPostsCount, viewed)
            markSecondaryAsRead(binding.hasNewPostsIndicator, viewed)
        }
    }

    class OtherTopicItemVH(private val binding: ItemSpecialTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val backgroundColor = binding.context.asTheme().colorBackground

        fun bind(topic: Topic, listener: TopicListAdapterListener) {
            /**
             * reset background color in case changed by [SwipeToUnFollowCallback]
             */
            binding.root.setBackgroundColor(backgroundColor)

            if (topic.mainBoard == null) {
                FirebaseCrashlytics.getInstance().log("MainBoard doesn't exist for '$topic'")
                return
            }

            binding.root.setOnClickListener { listener.onTopicClicked(topic) }
            binding.storyTitle.text = topic.title
            binding.txtBoard.text = topic.mainBoard!!.name
            binding.txtBoard.setOnClickListener { listener.onBoardClicked(topic.mainBoard!!) }
            binding.storyAuthor.text = topic.author!!.name
            binding.storyAuthor.setOnClickListener { listener.onAuthorClicked(topic.author!!) }
            binding.hasNewPostsIndicator.isVisible = topic.hasNewPosts
            binding.txtPostsCount.text = topic.postCount.toString()

            val viewed = listener.hasViewedTopic(topic)
            markPrimaryAsRead(binding.storyTitle, viewed)
            markSecondaryAsRead(binding.storyAuthor, viewed)
            markSecondaryAsRead(binding.txtBoard, viewed)
            markSecondaryAsRead(binding.txtPostsCount, viewed)
            markSecondaryAsRead(binding.hasNewPostsIndicator, viewed)
        }
    }


    class FeaturedTopicItemVH(private val binding: ItemFeaturedTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(topic: Topic, listener: TopicListAdapterListener) {
            binding.root.setOnClickListener { listener.onTopicClicked(topic) }
            binding.storyTitle.text = topic.title
//            binding.storyAuthor.text = topic.author!!.name
            binding.storyAuthor.setOnClickListener { listener.onAuthorClicked(topic.author!!) }

            val viewed = listener.hasViewedTopic(topic)
            markPrimaryAsRead(binding.storyTitle, viewed)
            markSecondaryAsRead(binding.storyAuthor, viewed)
        }
    }

    class FooterVH(private val binding: ItemFooterBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(listener: TopicListAdapterListener) {
            binding.button.isEnabled = if (listener.hasNextPage()) {
                binding.button.setText(R.string.next_page)
                binding.button.setOnClickListener { listener.loadNextPage() }
                true
            } else {
                binding.button.setText(R.string.end_of_topics)
                false
            }
        }
    }


    companion object {
        const val ITEM_BOARD_TOPIC = 0
        const val ITEM_OTHER_TOPIC = 1
        const val ITEM_FEATURED_TOPIC = 2
        const val ITEM_FOOTER = 3

        private fun markPrimaryAsRead(view: TextView, hasBeenRead: Boolean) {
            val theme = view.context.asTheme()
            view.setTextColor(if (!hasBeenRead) theme.textColorPrimary else theme.textColorTertiary)
        }

        private fun markSecondaryAsRead(view: TextView, hasBeenRead: Boolean) {
            val theme = view.context.asTheme()
            view.setTextColor(if (!hasBeenRead) theme.textColorPrimary else theme.textColorTertiary)
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = when {
                oldItem is TopicItem && newItem is TopicItem -> oldItem.topic.id == newItem.topic.id
                else -> false
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem == newItem

        }
    }
}

sealed class Item
private sealed class TopicItem(open val topic: Topic) : Item()
private data class BoardTopicItem(override val topic: Topic) : TopicItem(topic)
private data class FeaturedTopicItem(override val topic: Topic) : TopicItem(topic)
private data class OtherTopicItem(override val topic: Topic) : TopicItem(topic)
private object FooterItem : Item()

interface TopicListAdapterListener {
    fun onTopicClicked(topic: Topic)
    fun onAuthorClicked(user: User)
    fun onBoardClicked(board: Board)
    fun hasViewedTopic(topic: Topic): Boolean
    fun hasNextPage(): Boolean
    fun hasPrevPage(): Boolean
    fun loadNextPage()
    fun loadPrevPage()
    fun loadFirstPage()
    fun loadLastPage()
}
