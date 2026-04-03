import 'package:fronted/presentation/screens/content_creation/step2_photo_description.dart';
import 'package:fronted/presentation/screens/content_creation/step3_style_select.dart';
import 'package:fronted/presentation/screens/content_creation/step4_preview_generate.dart';
import 'package:fronted/presentation/screens/processing/processing_loading.dart';
import 'package:fronted/presentation/screens/result_view/result_view.dart';
import 'package:go_router/go_router.dart';

import '../presentation/screens/splash/splash_screen.dart';
import '../presentation/screens/login/login_screen.dart';
import '../presentation/screens/dashboard/dashboard_screen.dart';
import '../presentation/screens/profile_setup/profile_setup_screen.dart';
import '../presentation/screens/content_creation/step1_photo_upload.dart';
import '../presentation/screens/history/history_screen.dart';
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
      path: '/address-search',
      builder: (context, state) => const AddressSearchScreen(),
    ),

    GoRoute(
      path: '/map-location-picker',
      builder: (context, state) => const MapLocationPickerScreen(),
    ),

    GoRoute(
      path: '/dashboard',
      builder:(context, state) => const DashboardScreen(),
    ),

    GoRoute(
      path: '/step1',
      builder:(context, state) => const Step1PhotoUpload(),
    ),

    GoRoute(
      path: '/step2',
      builder:(context, state) => const Step2PhotoDescription(),
    ),

    GoRoute(
      path: '/step3',
      builder:(context, state) => const Step3StyleSelect(),
    ),

    GoRoute(
      path: '/step4',
      builder:(context, state) => const Step4PreviewGenerate(),
    ),

    GoRoute(
      path: '/processing',
      builder:(context, state) => const ProcessingLoadingScreen(),
    ),

    GoRoute(
      path: '/result',
      builder:(context, state) => const ResultViewScreen(),
    ),

    GoRoute(
      path: '/history',
      builder: (context, state) => const HistoryScreen(),
    ),
  ],
);