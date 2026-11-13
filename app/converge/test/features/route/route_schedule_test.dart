// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/route/data/models/route_schedule.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('a chegada passa da meia-noite sem quebrar o formato', () {
    final schedule = RouteSchedule.startingAt(DateTime(2026, 11, 15, 23, 40));

    expect(schedule.departureTime, '23:40');
    expect(schedule.estimatedArrivalTime, '00:25');
    expect(schedule.weekDays, {'SUNDAY'});
  });

}
