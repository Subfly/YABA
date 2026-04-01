package dev.subfly.yaba.util

import dev.subfly.yaba.R

import androidx.annotation.StringRes

import dev.subfly.yaba.core.icons.IconCategory

/** Maps IconCategory ID to its localized string resource id. */
@StringRes
fun IconCategory.localizedNameRes(): Int =
        when (id) {
            "business_finance" -> R.string.icon_category_business_finance
            "technology_computing" -> R.string.icon_category_technology_computing
            "design_media" -> R.string.icon_category_design_media
            "system_files" -> R.string.icon_category_system_files
            "navigation_actions" -> R.string.icon_category_navigation_actions
            "lifestyle_personal" -> R.string.icon_category_lifestyle_personal
            "education_learning" -> R.string.icon_category_education_learning
            "entertainment_communication" -> R.string.icon_category_entertainment_communication
            else -> R.string.icon_category_business_finance // Fallback
        }

/** Maps IconCategory ID to its localized description string resource id. */
@StringRes
fun IconCategory.localizedDescriptionRes(): Int =
        when (id) {
            "business_finance" -> R.string.icon_category_business_finance_description
            "technology_computing" -> R.string.icon_category_technology_computing_description
            "design_media" -> R.string.icon_category_design_media_description
            "system_files" -> R.string.icon_category_system_files_description
            "navigation_actions" -> R.string.icon_category_navigation_actions_description
            "lifestyle_personal" -> R.string.icon_category_lifestyle_personal_description
            "education_learning" -> R.string.icon_category_education_learning_description
            "entertainment_communication" ->
                    R.string.icon_category_entertainment_communication_description
            else -> R.string.icon_category_business_finance_description // Fallback
        }

