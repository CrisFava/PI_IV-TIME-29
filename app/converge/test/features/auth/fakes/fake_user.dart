import 'package:converge/features/auth/data/models/academic_record_model.dart';
import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/auth/data/models/user_model.dart';

UserModel fakeUser({String uid = 'uid-1', String email = 'ana@puc.edu.br'}) =>
    UserModel(
      id: 'id-1',
      firebaseUid: uid,
      name: 'Ana Souza',
      email: email,
      phone: '(19) 91234-5678',
      photoUrl: null,
      participationRoles: const {ParticipationRole.passenger},
      academicRecord: AcademicRecordModel(
        university: 'PUC-Campinas',
        course: 'Eng. Software',
        enrollmentNumber: '2024-00123',
        institutionalEmail: email,
      ),
      active: true,
    );
