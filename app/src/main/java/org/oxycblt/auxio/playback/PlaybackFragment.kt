package org.oxycblt.auxio.playback

import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.oxycblt.auxio.R
import org.oxycblt.auxio.databinding.FragmentPlaybackBinding
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.logD
import org.oxycblt.auxio.playback.state.LoopMode
import org.oxycblt.auxio.ui.Accent
import org.oxycblt.auxio.ui.memberBinding
import org.oxycblt.auxio.ui.toStateList

/**
 * A [Fragment] that displays more information about the song, along with more media controls.
 * Instantiation is done by the navigation component, **do not instantiate this fragment manually.**
 * @author OxygenCobalt
 */
class PlaybackFragment : Fragment(), SeekBar.OnSeekBarChangeListener {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    private val binding by memberBinding(FragmentPlaybackBinding::inflate) {
        playbackSong.isSelected = false
    }

    // Colors
    private val accentColor: ColorStateList by lazy {
        Accent.get().getStateList(requireContext())
    }

    private val controlColor: ColorStateList by lazy {
        R.color.control_color.toStateList(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // TODO: Add a swipe-to-next-track function using a ViewPager
        //  Would require writing my own variant though to avoid index updates

        val normalTextColor = binding.playbackDurationCurrent.currentTextColor

        // Can't set the tint of a MenuItem below Android 8, so use icons instead.
        val iconQueueActive = ContextCompat.getDrawable(
            requireContext(), R.drawable.ic_queue
        )

        val iconQueueInactive = ContextCompat.getDrawable(
            requireContext(), R.drawable.ic_queue_inactive
        )

        val queueMenuItem: MenuItem

        // --- UI SETUP ---

        binding.lifecycleOwner = viewLifecycleOwner
        binding.playbackModel = playbackModel
        binding.detailModel = detailModel
        binding.song = playbackModel.song.value!!

        binding.playbackToolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_queue) {
                    findNavController().navigate(PlaybackFragmentDirections.actionShowQueue())

                    true
                } else false
            }

            queueMenuItem = menu.findItem(R.id.action_queue)
        }

        // Make marquee of song title work
        binding.playbackSong.isSelected = true
        binding.playbackSeekBar.setOnSeekBarChangeListener(this)

        // --- VIEWMODEL SETUP --

        playbackModel.song.observe(viewLifecycleOwner) {
            if (it != null) {
                logD("Updating song display to ${it.name}.")

                binding.song = it
                binding.playbackSeekBar.max = it.seconds.toInt()
            } else {
                logD("No song is being played, leaving.")

                findNavController().navigateUp()
            }
        }

        playbackModel.isShuffling.observe(viewLifecycleOwner) {
            // Highlight the shuffle button if Playback is shuffled, and revert it if not.
            if (it) {
                binding.playbackShuffle.imageTintList = accentColor
            } else {
                binding.playbackShuffle.imageTintList = controlColor
            }
        }

        playbackModel.loopMode.observe(viewLifecycleOwner) {
            when (it) {
                LoopMode.NONE -> {
                    binding.playbackLoop.imageTintList = controlColor
                    binding.playbackLoop.setImageResource(R.drawable.ic_loop)
                }
                LoopMode.ONCE -> {
                    binding.playbackLoop.imageTintList = accentColor
                    binding.playbackLoop.setImageResource(R.drawable.ic_loop_one)
                }
                LoopMode.INFINITE -> {
                    binding.playbackLoop.imageTintList = accentColor
                    binding.playbackLoop.setImageResource(R.drawable.ic_loop)
                }

                else -> return@observe
            }
        }

        playbackModel.isSeeking.observe(viewLifecycleOwner) {
            // Highlight the current duration if the user is seeking, and revert it if not.
            if (it) {
                binding.playbackDurationCurrent.setTextColor(accentColor)
            } else {
                binding.playbackDurationCurrent.setTextColor(normalTextColor)
            }
        }

        // Updates for the current duration TextView/SeekBar
        playbackModel.formattedPosition.observe(viewLifecycleOwner) {
            binding.playbackDurationCurrent.text = it
        }

        playbackModel.positionAsProgress.observe(viewLifecycleOwner) {
            if (!playbackModel.isSeeking.value!!) {
                binding.playbackSeekBar.progress = it
            }
        }

        playbackModel.nextItemsInQueue.observe(viewLifecycleOwner) {
            // Disable the option to open the queue if there's nothing in it.
            if (it.isEmpty() && playbackModel.userQueue.value!!.isEmpty()) {
                queueMenuItem.isEnabled = false
                queueMenuItem.icon = iconQueueInactive
            } else {
                queueMenuItem.isEnabled = true
                queueMenuItem.icon = iconQueueActive
            }
        }

        playbackModel.userQueue.observe(viewLifecycleOwner) {
            if (it.isEmpty() && playbackModel.nextItemsInQueue.value!!.isEmpty()) {
                queueMenuItem.isEnabled = false
                queueMenuItem.icon = iconQueueInactive
            } else {
                queueMenuItem.isEnabled = true
                queueMenuItem.icon = iconQueueActive
            }
        }

        detailModel.navToItem.observe(viewLifecycleOwner) {
            if (it != null) {
                findNavController().navigateUp()
            }
        }

        logD("Fragment Created.")

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        playbackModel.disableAnimation()

        val iconPauseToPlay = ContextCompat.getDrawable(
            requireContext(), R.drawable.ic_pause_to_play
        ) as AnimatedVectorDrawable

        val iconPlayToPause = ContextCompat.getDrawable(
            requireContext(), R.drawable.ic_play_to_pause
        ) as AnimatedVectorDrawable

        playbackModel.isPlaying.observe(viewLifecycleOwner) {
            if (it) {
                if (playbackModel.canAnimate) {
                    binding.playbackPlayPause.setImageDrawable(iconPlayToPause)
                    iconPlayToPause.start()
                } else {
                    // Use a static icon the first time around to fix premature animation
                    // [Which looks weird]
                    binding.playbackPlayPause.setImageResource(R.drawable.ic_pause_large)

                    playbackModel.enableAnimation()
                }

                binding.playbackPlayPause.backgroundTintList = accentColor
            } else {
                if (playbackModel.canAnimate) {
                    binding.playbackPlayPause.setImageDrawable(iconPauseToPlay)
                    iconPauseToPlay.start()
                } else {
                    binding.playbackPlayPause.setImageResource(R.drawable.ic_play_large)

                    playbackModel.enableAnimation()
                }

                binding.playbackPlayPause.backgroundTintList = controlColor
            }
        }
    }

    // --- SEEK CALLBACKS ---

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            // Only update the display when seeking, as to have PlaybackService seek
            // [causing possible buffering] on every movement is really odd.
            playbackModel.updatePositionDisplay(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        playbackModel.setSeekingStatus(true)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        playbackModel.setSeekingStatus(false)

        // Confirm the position when seeking stops.
        playbackModel.setPosition(seekBar.progress)
    }
}
