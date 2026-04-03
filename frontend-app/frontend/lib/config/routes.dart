import 'package:fronted/presentation/screens/content_creation/step2_photo_description.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import '../presentation/screens/splash/splash_screen.dart';
import '../presentation/screens/login/login_screen.dart';
import '../presentation/screens/dashboard/dashboard_screen.dart';
import '../presentation/screens/profile_setup/profile_setup_screen.dart';
import '../presentation/screens/content_creation/step1_photo_upload.dart';
import '../presentation/screens/history/history_screen.dart';

final router = GoRouter(
  initialLocation: '/dashboard',
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
      path: '/dashboard',
      builder:(context, state) => const DashboardScreen(),
    ),

    GoRoute(
      path: '/step1',
      builder:(context, state) => const Step1PhotoUpload(),
    ),

    GoRoute(
      path: '/step2',
      builder:(context, state) {
        final images = (state.extra as List<XFile>?) ?? [];
        return Step2PhotoDescription(images: images);
      },
    ),

    GoRoute(
      path: '/history',
      builder: (context, state) => const HistoryScreen(),
    ),
  ],
);