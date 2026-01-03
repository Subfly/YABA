package dev.subfly.yaba.util

import dev.subfly.yabacore.icons.IconCategory
import dev.subfly.yabacore.icons.IconSubcategory
import org.jetbrains.compose.resources.StringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.icon_category_business_finance
import yaba.composeapp.generated.resources.icon_category_business_finance_description
import yaba.composeapp.generated.resources.icon_category_design_media
import yaba.composeapp.generated.resources.icon_category_design_media_description
import yaba.composeapp.generated.resources.icon_category_education_learning
import yaba.composeapp.generated.resources.icon_category_education_learning_description
import yaba.composeapp.generated.resources.icon_category_entertainment_communication
import yaba.composeapp.generated.resources.icon_category_entertainment_communication_description
import yaba.composeapp.generated.resources.icon_category_lifestyle_personal
import yaba.composeapp.generated.resources.icon_category_lifestyle_personal_description
import yaba.composeapp.generated.resources.icon_category_navigation_actions
import yaba.composeapp.generated.resources.icon_category_navigation_actions_description
import yaba.composeapp.generated.resources.icon_category_system_files
import yaba.composeapp.generated.resources.icon_category_system_files_description
import yaba.composeapp.generated.resources.icon_category_technology_computing
import yaba.composeapp.generated.resources.icon_category_technology_computing_description
import yaba.composeapp.generated.resources.icon_subcategory_academic_education
import yaba.composeapp.generated.resources.icon_subcategory_academic_education_description
import yaba.composeapp.generated.resources.icon_subcategory_branding_symbols
import yaba.composeapp.generated.resources.icon_subcategory_branding_symbols_description
import yaba.composeapp.generated.resources.icon_subcategory_business_operations
import yaba.composeapp.generated.resources.icon_subcategory_business_operations_description
import yaba.composeapp.generated.resources.icon_subcategory_communication_social
import yaba.composeapp.generated.resources.icon_subcategory_communication_social_description
import yaba.composeapp.generated.resources.icon_subcategory_data_operations
import yaba.composeapp.generated.resources.icon_subcategory_data_operations_description
import yaba.composeapp.generated.resources.icon_subcategory_devices_hardware
import yaba.composeapp.generated.resources.icon_subcategory_devices_hardware_description
import yaba.composeapp.generated.resources.icon_subcategory_editing_tools
import yaba.composeapp.generated.resources.icon_subcategory_editing_tools_description
import yaba.composeapp.generated.resources.icon_subcategory_files_storage
import yaba.composeapp.generated.resources.icon_subcategory_files_storage_description
import yaba.composeapp.generated.resources.icon_subcategory_finance_crypto
import yaba.composeapp.generated.resources.icon_subcategory_finance_crypto_description
import yaba.composeapp.generated.resources.icon_subcategory_food_kitchen
import yaba.composeapp.generated.resources.icon_subcategory_food_kitchen_description
import yaba.composeapp.generated.resources.icon_subcategory_games_entertainment
import yaba.composeapp.generated.resources.icon_subcategory_games_entertainment_description
import yaba.composeapp.generated.resources.icon_subcategory_health_fitness
import yaba.composeapp.generated.resources.icon_subcategory_health_fitness_description
import yaba.composeapp.generated.resources.icon_subcategory_home_living
import yaba.composeapp.generated.resources.icon_subcategory_home_living_description
import yaba.composeapp.generated.resources.icon_subcategory_legal_compliance
import yaba.composeapp.generated.resources.icon_subcategory_legal_compliance_description
import yaba.composeapp.generated.resources.icon_subcategory_maps_logistics
import yaba.composeapp.generated.resources.icon_subcategory_maps_logistics_description
import yaba.composeapp.generated.resources.icon_subcategory_mathematics
import yaba.composeapp.generated.resources.icon_subcategory_mathematics_description
import yaba.composeapp.generated.resources.icon_subcategory_media_content
import yaba.composeapp.generated.resources.icon_subcategory_media_content_description
import yaba.composeapp.generated.resources.icon_subcategory_navigation_arrows
import yaba.composeapp.generated.resources.icon_subcategory_navigation_arrows_description
import yaba.composeapp.generated.resources.icon_subcategory_programming_ai
import yaba.composeapp.generated.resources.icon_subcategory_programming_ai_description
import yaba.composeapp.generated.resources.icon_subcategory_reference_notes
import yaba.composeapp.generated.resources.icon_subcategory_reference_notes_description
import yaba.composeapp.generated.resources.icon_subcategory_system_settings
import yaba.composeapp.generated.resources.icon_subcategory_system_settings_description
import yaba.composeapp.generated.resources.icon_subcategory_user_actions
import yaba.composeapp.generated.resources.icon_subcategory_user_actions_description
import yaba.composeapp.generated.resources.icon_subcategory_world_environment
import yaba.composeapp.generated.resources.icon_subcategory_world_environment_description

/** Maps IconCategory ID to its localized name StringResource. */
fun IconCategory.localizedNameRes(): StringResource =
        when (id) {
            "business_finance" -> Res.string.icon_category_business_finance
            "technology_computing" -> Res.string.icon_category_technology_computing
            "design_media" -> Res.string.icon_category_design_media
            "system_files" -> Res.string.icon_category_system_files
            "navigation_actions" -> Res.string.icon_category_navigation_actions
            "lifestyle_personal" -> Res.string.icon_category_lifestyle_personal
            "education_learning" -> Res.string.icon_category_education_learning
            "entertainment_communication" -> Res.string.icon_category_entertainment_communication
            else -> Res.string.icon_category_business_finance // Fallback
        }

/** Maps IconCategory ID to its localized description StringResource. */
fun IconCategory.localizedDescriptionRes(): StringResource =
        when (id) {
            "business_finance" -> Res.string.icon_category_business_finance_description
            "technology_computing" -> Res.string.icon_category_technology_computing_description
            "design_media" -> Res.string.icon_category_design_media_description
            "system_files" -> Res.string.icon_category_system_files_description
            "navigation_actions" -> Res.string.icon_category_navigation_actions_description
            "lifestyle_personal" -> Res.string.icon_category_lifestyle_personal_description
            "education_learning" -> Res.string.icon_category_education_learning_description
            "entertainment_communication" ->
                    Res.string.icon_category_entertainment_communication_description
            else -> Res.string.icon_category_business_finance_description // Fallback
        }

/** Maps IconSubcategory ID to its localized name StringResource. */
fun IconSubcategory.localizedNameRes(): StringResource =
        when (id) {
            // Business & Finance
            "business_operations" -> Res.string.icon_subcategory_business_operations
            "finance_crypto" -> Res.string.icon_subcategory_finance_crypto
            "legal_compliance" -> Res.string.icon_subcategory_legal_compliance
            // Technology & Computing
            "devices_hardware" -> Res.string.icon_subcategory_devices_hardware
            "programming_ai" -> Res.string.icon_subcategory_programming_ai
            // Design & Media
            "editing_tools" -> Res.string.icon_subcategory_editing_tools
            "media_content" -> Res.string.icon_subcategory_media_content
            // System & Files
            "files_storage" -> Res.string.icon_subcategory_files_storage
            "system_settings" -> Res.string.icon_subcategory_system_settings
            // Navigation & Actions
            "navigation_arrows" -> Res.string.icon_subcategory_navigation_arrows
            "user_actions" -> Res.string.icon_subcategory_user_actions
            "data_operations" -> Res.string.icon_subcategory_data_operations
            "maps_logistics" -> Res.string.icon_subcategory_maps_logistics
            // Lifestyle & Personal
            "health_fitness" -> Res.string.icon_subcategory_health_fitness
            "food_kitchen" -> Res.string.icon_subcategory_food_kitchen
            "home_living" -> Res.string.icon_subcategory_home_living
            // Education & Learning
            "academic_education" -> Res.string.icon_subcategory_academic_education
            "mathematics" -> Res.string.icon_subcategory_mathematics
            "reference_notes" -> Res.string.icon_subcategory_reference_notes
            // Entertainment & Communication
            "games_entertainment" -> Res.string.icon_subcategory_games_entertainment
            "communication_social" -> Res.string.icon_subcategory_communication_social
            "world_environment" -> Res.string.icon_subcategory_world_environment
            "branding_symbols" -> Res.string.icon_subcategory_branding_symbols
            else -> Res.string.icon_subcategory_business_operations // Fallback
        }

/** Maps IconSubcategory ID to its localized description StringResource. */
fun IconSubcategory.localizedDescriptionRes(): StringResource =
        when (id) {
            // Business & Finance
            "business_operations" -> Res.string.icon_subcategory_business_operations_description
            "finance_crypto" -> Res.string.icon_subcategory_finance_crypto_description
            "legal_compliance" -> Res.string.icon_subcategory_legal_compliance_description
            // Technology & Computing
            "devices_hardware" -> Res.string.icon_subcategory_devices_hardware_description
            "programming_ai" -> Res.string.icon_subcategory_programming_ai_description
            // Design & Media
            "editing_tools" -> Res.string.icon_subcategory_editing_tools_description
            "media_content" -> Res.string.icon_subcategory_media_content_description
            // System & Files
            "files_storage" -> Res.string.icon_subcategory_files_storage_description
            "system_settings" -> Res.string.icon_subcategory_system_settings_description
            // Navigation & Actions
            "navigation_arrows" -> Res.string.icon_subcategory_navigation_arrows_description
            "user_actions" -> Res.string.icon_subcategory_user_actions_description
            "data_operations" -> Res.string.icon_subcategory_data_operations_description
            "maps_logistics" -> Res.string.icon_subcategory_maps_logistics_description
            // Lifestyle & Personal
            "health_fitness" -> Res.string.icon_subcategory_health_fitness_description
            "food_kitchen" -> Res.string.icon_subcategory_food_kitchen_description
            "home_living" -> Res.string.icon_subcategory_home_living_description
            // Education & Learning
            "academic_education" -> Res.string.icon_subcategory_academic_education_description
            "mathematics" -> Res.string.icon_subcategory_mathematics_description
            "reference_notes" -> Res.string.icon_subcategory_reference_notes_description
            // Entertainment & Communication
            "games_entertainment" -> Res.string.icon_subcategory_games_entertainment_description
            "communication_social" -> Res.string.icon_subcategory_communication_social_description
            "world_environment" -> Res.string.icon_subcategory_world_environment_description
            "branding_symbols" -> Res.string.icon_subcategory_branding_symbols_description
            else -> Res.string.icon_subcategory_business_operations_description // Fallback
        }
