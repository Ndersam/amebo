package com.amebo.amebo.screens.postlist.mentions

import android.app.Application
import com.amebo.amebo.common.Pref
import com.amebo.amebo.screens.postlist.PostListScreenViewModel
import com.amebo.core.Nairaland
import com.amebo.core.domain.Mentions
import javax.inject.Inject

class MentionsViewModel @Inject constructor(server: Nairaland, pref: Pref, application: Application) :
    PostListScreenViewModel<Mentions>(server, pref, application)
