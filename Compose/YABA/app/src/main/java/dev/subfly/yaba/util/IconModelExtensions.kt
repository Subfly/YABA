package dev.subfly.yaba.util

import dev.subfly.yaba.R

import androidx.annotation.StringRes

import dev.subfly.yaba.core.icons.IconCategory
import dev.subfly.yaba.core.icons.IconSubcategory

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

/** Maps IconSubcategory ID to its localized string resource id. */
@StringRes
fun IconSubcategory.localizedNameRes(): Int =
        when (id) {
            // Business & Finance
            "business_operations" -> R.string.icon_subcategory_business_operations
            "finance_crypto" -> R.string.icon_subcategory_finance_crypto
            "legal_compliance" -> R.string.icon_subcategory_legal_compliance
            // Technology & Computing
            "devices_hardware" -> R.string.icon_subcategory_devices_hardware
            "programming_ai" -> R.string.icon_subcategory_programming_ai
            // Design & Media
            "editing_tools" -> R.string.icon_subcategory_editing_tools
            "media_content" -> R.string.icon_subcategory_media_content
            // System & Files
            "files_storage" -> R.string.icon_subcategory_files_storage
            "system_settings" -> R.string.icon_subcategory_system_settings
            // Navigation & Actions
            "navigation_arrows" -> R.string.icon_subcategory_navigation_arrows
            "user_actions" -> R.string.icon_subcategory_user_actions
            "data_operations" -> R.string.icon_subcategory_data_operations
            "maps_logistics" -> R.string.icon_subcategory_maps_logistics
            // Lifestyle & Personal
            "health_fitness" -> R.string.icon_subcategory_health_fitness
            "food_kitchen" -> R.string.icon_subcategory_food_kitchen
            "home_living" -> R.string.icon_subcategory_home_living
            // Education & Learning
            "academic_education" -> R.string.icon_subcategory_academic_education
            "mathematics" -> R.string.icon_subcategory_mathematics
            "reference_notes" -> R.string.icon_subcategory_reference_notes
            // Entertainment & Communication
            "games_entertainment" -> R.string.icon_subcategory_games_entertainment
            "communication_social" -> R.string.icon_subcategory_communication_social
            "world_environment" -> R.string.icon_subcategory_world_environment
            "branding_symbols" -> R.string.icon_subcategory_branding_symbols
            else -> R.string.icon_subcategory_business_operations // Fallback
        }

/** Maps IconSubcategory ID to its localized description string resource id. */
@StringRes
fun IconSubcategory.localizedDescriptionRes(): Int =
        when (id) {
            // Business & Finance
            "business_operations" -> R.string.icon_subcategory_business_operations_description
            "finance_crypto" -> R.string.icon_subcategory_finance_crypto_description
            "legal_compliance" -> R.string.icon_subcategory_legal_compliance_description
            // Technology & Computing
            "devices_hardware" -> R.string.icon_subcategory_devices_hardware_description
            "programming_ai" -> R.string.icon_subcategory_programming_ai_description
            // Design & Media
            "editing_tools" -> R.string.icon_subcategory_editing_tools_description
            "media_content" -> R.string.icon_subcategory_media_content_description
            // System & Files
            "files_storage" -> R.string.icon_subcategory_files_storage_description
            "system_settings" -> R.string.icon_subcategory_system_settings_description
            // Navigation & Actions
            "navigation_arrows" -> R.string.icon_subcategory_navigation_arrows_description
            "user_actions" -> R.string.icon_subcategory_user_actions_description
            "data_operations" -> R.string.icon_subcategory_data_operations_description
            "maps_logistics" -> R.string.icon_subcategory_maps_logistics_description
            // Lifestyle & Personal
            "health_fitness" -> R.string.icon_subcategory_health_fitness_description
            "food_kitchen" -> R.string.icon_subcategory_food_kitchen_description
            "home_living" -> R.string.icon_subcategory_home_living_description
            // Education & Learning
            "academic_education" -> R.string.icon_subcategory_academic_education_description
            "mathematics" -> R.string.icon_subcategory_mathematics_description
            "reference_notes" -> R.string.icon_subcategory_reference_notes_description
            // Entertainment & Communication
            "games_entertainment" -> R.string.icon_subcategory_games_entertainment_description
            "communication_social" -> R.string.icon_subcategory_communication_social_description
            "world_environment" -> R.string.icon_subcategory_world_environment_description
            "branding_symbols" -> R.string.icon_subcategory_branding_symbols_description
            else -> R.string.icon_subcategory_business_operations_description // Fallback
        }
