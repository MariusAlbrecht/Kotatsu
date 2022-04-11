package org.koitharu.kotatsu.favourites.ui.list

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.list.domain.CountersProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class FavouritesListViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository,
	private val trackingRepository: TrackingRepository,
	settings: AppSettings,
) : MangaListViewModel(settings), CountersProvider {

	override val content = combine(
		if (categoryId == 0L) {
			repository.observeAll(SortOrder.NEWEST)
		} else {
			repository.observeAll(categoryId)
		},
		createListModeFlow()
	) { list, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_heart_outline,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = if (categoryId == 0L) {
						R.string.you_have_not_favourites_yet
					} else {
						R.string.favourites_category_empty
					},
					actionStringRes = 0,
				)
			)
			else -> list.toUi(mode, this)
		}
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun removeFromFavourites(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob {
			if (categoryId == 0L) {
				repository.removeFromFavourites(ids)
			} else {
				repository.removeFromCategory(categoryId, ids)
			}
		}
	}

	override suspend fun getCounter(mangaId: Long): Int {
		return trackingRepository.getNewChaptersCount(mangaId)
	}
}