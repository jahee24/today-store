enum StyleType {
  clean,
  emotional,
  formal,
  friendly,
}

extension StyleTypeX on StyleType {
  String get apiValue {
    switch (this) {
      case StyleType.clean:
      return 'clean';
      case StyleType.emotional:
      return 'emotional';
      case StyleType.formal:
      return 'formal';
      case StyleType.friendly:
      return 'friendly';
    }
  }

  String get label {
    switch (this) {
      case StyleType.clean:
      return '깔끔';
      case StyleType.emotional:
      return '감성적';
      case StyleType.formal:
      return '전문성';
      case StyleType.friendly:
      return '친근';
    }
  }

  static StyleType? fromLabel(String? label) {
    switch (label) {
      case '깔끔':
      return StyleType.clean;
      case '무난':
      case '감성적':
      return StyleType.emotional;
      case '전문성':
      return StyleType.formal;
      case '친근':
      return StyleType.friendly;
      default:
      return null;
    }
  }
}