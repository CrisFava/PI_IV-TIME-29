// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/auth/data/models/academic_record_model.dart';
import 'package:converge/features/auth/data/models/participation_role.dart';
import 'package:converge/features/auth/data/models/user_model.dart';

UserModel fakeChatUser({required String id, required String name}) => UserModel(
  id: id,
  firebaseUid: 'uid-$id',
  name: name,
  email: '$id@puc-campinas.edu.br',
  phone: '(19) 91234-5678',
  photoUrl: null,
  participationRoles: const {ParticipationRole.passenger},
  academicRecord: AcademicRecordModel(
    university: 'PUC-Campinas',
    course: 'Eng. Software',
    enrollmentNumber: '2024-00123',
    institutionalEmail: '$id@puc-campinas.edu.br',
  ),
  active: true,
);
