package org.oxycblt.auxio.detail.adapters

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.oxycblt.auxio.databinding.ItemArtistAlbumBinding
import org.oxycblt.auxio.databinding.ItemArtistHeaderBinding
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.music.Album
import org.oxycblt.auxio.music.Artist
import org.oxycblt.auxio.music.BaseModel
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.recycler.DiffCallback
import org.oxycblt.auxio.recycler.viewholders.BaseViewHolder
import org.oxycblt.auxio.recycler.viewholders.Highlightable
import org.oxycblt.auxio.ui.Accent
import org.oxycblt.auxio.ui.applyAccents
import org.oxycblt.auxio.ui.disable
import org.oxycblt.auxio.ui.inflater
import org.oxycblt.auxio.ui.setTextColorResource

/**
 * An adapter for displaying the [Album]s of an artist.
 */
class ArtistDetailAdapter(
    private val detailModel: DetailViewModel,
    private val playbackModel: PlaybackViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val doOnClick: (data: Album) -> Unit,
    private val doOnLongClick: (view: View, data: Album) -> Unit,
) : ListAdapter<BaseModel, RecyclerView.ViewHolder>(DiffCallback()) {
    private var currentAlbum: Album? = null
    private var lastHolder: Highlightable? = null

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Artist -> ARTIST_HEADER_ITEM_TYPE
            is Album -> ARTIST_ALBUM_ITEM_TYPE

            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ARTIST_HEADER_ITEM_TYPE -> ArtistHeaderViewHolder(
                ItemArtistHeaderBinding.inflate(parent.context.inflater)
            )

            ARTIST_ALBUM_ITEM_TYPE -> ArtistAlbumViewHolder(
                ItemArtistAlbumBinding.inflate(parent.context.inflater)
            )

            else -> error("Invalid ViewHolder item type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Artist -> (holder as ArtistHeaderViewHolder).bind(item)
            is Album -> (holder as ArtistAlbumViewHolder).bind(item)
        }

        if (currentAlbum != null && position > 0) {
            if (getItem(position).id == currentAlbum?.id) {
                // Reset the last ViewHolder before assigning the new, correct one to be highlighted
                lastHolder?.setHighlighted(false)
                lastHolder = (holder as Highlightable)
                holder.setHighlighted(true)
            } else {
                (holder as Highlightable).setHighlighted(false)
            }
        }
    }

    /**
     * Update the current album that this adapter should be watching for to highlight.
     * @param album The [Album] to highlight if found, null to clear any highlighted ViewHolders
     */
    fun setCurrentAlbum(album: Album?, recycler: RecyclerView) {
        // Clear out the last ViewHolder as a song update usually signifies that this current
        // ViewHolder is likely invalid.
        lastHolder?.setHighlighted(false)
        lastHolder = null

        currentAlbum = album

        if (album != null) {
            // Use existing data instead of having to re-sort it.
            val pos = currentList.indexOfFirst {
                it.name == album.name && it is Album
            }

            // Check if the ViewHolder if this album is visible, and highlight it if so.
            recycler.layoutManager?.findViewByPosition(pos)?.let { child ->
                recycler.getChildViewHolder(child)?.let {
                    lastHolder = it as Highlightable

                    lastHolder?.setHighlighted(true)
                }
            }
        }
    }

    inner class ArtistHeaderViewHolder(
        private val binding: ItemArtistHeaderBinding
    ) : BaseViewHolder<Artist>(binding) {

        override fun onBind(data: Artist) {
            binding.artist = data
            binding.detailModel = detailModel
            binding.playbackModel = playbackModel
            binding.lifecycleOwner = lifecycleOwner

            binding.artistShuffleButton.applyAccents(true)
            binding.artistPlayButton.applyAccents(false)

            if (data.albums.size < 2) {
                binding.artistSortButton.disable()
            }
        }
    }

    // Generic ViewHolder for a detail album
    inner class ArtistAlbumViewHolder(
        private val binding: ItemArtistAlbumBinding,
    ) : BaseViewHolder<Album>(binding, doOnClick, doOnLongClick), Highlightable {
        private val normalTextColor = binding.albumName.currentTextColor

        override fun onBind(data: Album) {
            binding.album = data

            binding.albumName.requestLayout()
        }

        override fun setHighlighted(isHighlighted: Boolean) {
            if (isHighlighted) {
                binding.albumName.setTextColorResource(Accent.get().color)
            } else {
                binding.albumName.setTextColor(normalTextColor)
            }
        }
    }

    companion object {
        const val ARTIST_HEADER_ITEM_TYPE = 0xA009
        const val ARTIST_ALBUM_ITEM_TYPE = 0xA00A
    }
}
