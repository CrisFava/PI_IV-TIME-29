// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import '../models/indicator_period.dart';
import '../models/methodology_item.dart';
import '../models/platform_indicator_model.dart';
import '../models/user_indicator_model.dart';

/// Contrato com a API de indicadores (`/indicators`).
abstract class IndicatorRepository {
  Future<UserIndicatorModel> myIndicators(IndicatorPeriod period);

  Future<PlatformIndicatorModel> platformIndicators(IndicatorPeriod period);

  Future<List<MethodologyItem>> methodology();
}
