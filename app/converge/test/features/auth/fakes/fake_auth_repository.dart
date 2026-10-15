import 'fake_user.dart';

import 'package:converge/core/network/problem_details.dart';
import 'package:converge/features/auth/data/models/create_user_request.dart';
import 'package:converge/features/auth/data/models/user_model.dart';
import 'package:converge/features/auth/data/repository/auth_repository.dart';

class FakeAuthRepository implements AuthRepository {
  UserModel? stored;
  ProblemDetailsException? fetchError;
  ProblemDetailsException? createError;
  final List<CreateUserRequest> created = [];

  FakeAuthRepository({this.stored});

  @override
  Future<UserModel> fetchCurrentUser() async {
    final error = fetchError;
    if (error != null) throw error;
    final user = stored;
    if (user == null) throw ProblemDetailsException.fromStatus(404);
    return user;
  }

  @override
  Future<UserModel> createUser(CreateUserRequest request) async {
    created.add(request);
    final error = createError;
    if (error != null) throw error;
    return stored = fakeUser(uid: request.firebaseUid, email: request.email);
  }
}
