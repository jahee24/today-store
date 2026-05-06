import 'package:fronted/presentation/screens/content_creation/step2_photo_description.dart';
import 'package:fronted/presentation/screens/content_creation/step3_style_select.dart';
import 'package:fronted/presentation/screens/content_creation/step4_preview_generate.dart';
import 'package:fronted/presentation/screens/image_content_creation/image_step1_photo_upload.dart';
import 'package:fronted/presentation/screens/processing/processing_loading.dart';
import 'package:fronted/presentation/screens/result_view/image_result_view.dart';
import 'package:fronted/presentation/screens/result_view/result_view.dart';
import 'package:fronted/presentation/screens/share_deeplinks/share_options_screen.dart';
import 'package:go_router/go_router.dart';

import '../presentation/screens/splash/splash_screen.dart';
import '../presentation/screens/login/login_screen.dart';
import '../presentation/screens/dashboard/dashboard_screen.dart';
import '../presentation/screens/profile_setup/profile_setup_screen.dart';
import '../presentation/screens/settings/profile_edit_screen.dart';
import '../presentation/screens/settings/sns_management_screen.dart';
import '../presentation/screens/content_creation/step1_photo_upload.dart';
import '../presentation/screens/history/history_screen.dart';
import '../presentation/screens/settings/settings_screen.dart';
import '../presentation/screens/address/map_location_picker_screen.dart';
import '../presentation/screens/address/address_search_screen.dart';

final router = GoRouter(
  initialLocation: '/',
  
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const SplashScreen(),
    ),

    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),

    GoRoute(
      path: '/profile-setup',
      builder: (context, state) => const ProfileSetupScreen(),
    ),

    GoRoute(
      path: '/profile-edit',
      builder: (context, state) => const ProfileEditScreen(),
    ),

    GoRoute(
      path: '/sns-management',
      builder: (context, state) => const SnsManagementScreen(),
    ),

    GoRoute(
      path: '/address-search',
      builder: (context, state) => const AddressSearchScreen(),
    ),

    GoRoute(
      path: '/map-location-picker',
      builder: (context, state) => const MapLocationPickerScreen(),
    ),

    GoRoute(
      path: '/dashboard',
      builder: (context, state) => const DashboardScreen(),
    ),

    GoRoute(
      path: '/step1',
      builder: (context, state) => const Step1PhotoUpload(),
    ),

    GoRoute(
      path: '/image-step1',
      builder: (context, state) => const ImageStep1PhotoUpload(),
    ),

    GoRoute(
      path: '/step2',
      builder: (context, state) => const Step2PhotoDescription(),
    ),

    GoRoute(
      path: '/step3',
      builder: (context, state) => const Step3StyleSelect(),
    ),

    GoRoute(
      path: '/step4',
      builder: (context, state) => const Step4PreviewGenerate(),
    ),

    GoRoute(
      path: '/processing',
      builder: (context, state) {
        final modeParam = state.uri.queryParameters['mode'];
        final mode = modeParam == 'image'
            ? ProcessingMode.image
            : ProcessingMode.text;
        return ProcessingLoadingScreen(mode: mode);
      },
    ),

    GoRoute(
      path: '/result',
      builder: (context, state) => const ResultViewScreen(),
    ),

    GoRoute(
      path: '/image-result',
      builder: (context, state) => const ImageResultViewScreen(),
    ),

    GoRoute(
      path: '/share',
      builder: (context, state) => const ShareOptionsScreen(),
    ),

    GoRoute(
      path: '/history',
      builder: (context, state) => const HistoryScreen(),
    ),

    GoRoute(
      path: '/settings',
      builder: (context, state) => const SettingsScreen(),
    ),
  ],
);
