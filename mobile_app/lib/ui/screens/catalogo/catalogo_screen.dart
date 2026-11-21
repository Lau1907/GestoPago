import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../config/app_theme.dart';
import '../../../core/utils/formatters.dart';
import '../../../models/gestopago/producto_item.dart';
import '../../../providers/catalogo_provider.dart';
import '../../widgets/empty_state_view.dart';
import 'producto_detalle_sheet.dart';

class CatalogoScreen extends StatefulWidget {
  const CatalogoScreen({super.key});

  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final catalogoProvider = context.read<CatalogoProvider>();
      if (catalogoProvider.productos.isEmpty) {
        catalogoProvider.cargarProductos();
      }
    });
  }

  void _abrirModalCompra(ProductoItem producto) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => ProductoDetalleSheet(producto: producto),
    );
  }

  IconData _obtenerIconoServicio(String servicio) {
    final lower = servicio.toLowerCase();
    if (lower.contains('agua')) return Icons.water_drop_outlined;
    if (lower.contains('luz') || lower.contains('cfe') || lower.contains('energia')) {
      return Icons.bolt_outlined;
    }
    if (lower.contains('gas')) return Icons.local_fire_department_outlined;
    if (lower.contains('tel') || lower.contains('movil') || lower.contains('recarga')) {
      return Icons.phone_android_outlined;
    }
    if (lower.contains('tv') || lower.contains('cable') || lower.contains('dish') || lower.contains('sky')) {
      return Icons.tv_outlined;
    }
    if (lower.contains('internet')) return Icons.wifi_outlined;
    return Icons.payments_outlined;
  }

  @override
  Widget build(BuildContext context) {
    final catalogoProvider = context.watch<CatalogoProvider>();

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => catalogoProvider.cargarProductos(),
        child: Column(
          children: [
            // Barra de búsqueda
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
              child: TextField(
                controller: _searchController,
                onChanged: catalogoProvider.filtrarPorTexto,
                decoration: InputDecoration(
                  hintText: 'Buscar servicios o productos...',
                  prefixIcon: const Icon(Icons.search, size: 20),
                  suffixIcon: _searchController.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear, size: 18),
                          onPressed: () {
                            _searchController.clear();
                            catalogoProvider.filtrarPorTexto('');
                          },
                        )
                      : null,
                  contentPadding: const EdgeInsets.symmetric(vertical: 12),
                ),
              ),
            ),

            // Chips horizontales de categorías / servicios
            if (catalogoProvider.categoriasDisponibles.isNotEmpty)
              SizedBox(
                height: 44,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: catalogoProvider.categoriasDisponibles.length + 1,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    if (index == 0) {
                      final isSelected = catalogoProvider.categoriaSeleccionada == null;
                      return ChoiceChip(
                        label: const Text('Todos'),
                        selected: isSelected,
                        onSelected: (_) => catalogoProvider.filtrarPorCategoria(null),
                      );
                    }
                    final cat = catalogoProvider.categoriasDisponibles[index - 1];
                    final isSelected = catalogoProvider.categoriaSeleccionada == cat;
                    return ChoiceChip(
                      label: Text(cat),
                      selected: isSelected,
                      onSelected: (_) => catalogoProvider.filtrarPorCategoria(cat),
                    );
                  },
                ),
              ),

            const SizedBox(height: 8),

            // Lista de productos o estados de carga/error
            Expanded(
              child: Builder(
                builder: (context) {
                  if (catalogoProvider.isLoading && catalogoProvider.productos.isEmpty) {
                    return const Center(child: CircularProgressIndicator());
                  }

                  if (catalogoProvider.errorMessage != null &&
                      catalogoProvider.productos.isEmpty) {
                    return EmptyStateView(
                      icon: Icons.cloud_off_outlined,
                      title: 'Error al consultar catálogo',
                      message: catalogoProvider.errorMessage!,
                      actionText: 'Reintentar',
                      onAction: () => catalogoProvider.cargarProductos(),
                    );
                  }

                  if (catalogoProvider.productos.isEmpty) {
                    return const EmptyStateView(
                      icon: Icons.search_off_outlined,
                      title: 'Sin productos disponibles',
                      message: 'No se encontraron servicios que coincidan con la búsqueda.',
                    );
                  }

                  return ListView.builder(
                    itemCount: catalogoProvider.productos.length,
                    padding: const EdgeInsets.only(bottom: 24),
                    itemBuilder: (context, index) {
                      final item = catalogoProvider.productos[index];
                      final iconData = _obtenerIconoServicio(item.servicio);

                      return Card(
                        child: InkWell(
                          borderRadius: BorderRadius.circular(16),
                          onTap: () => _abrirModalCompra(item),
                          child: Padding(
                            padding: const EdgeInsets.all(16.0),
                            child: Row(
                              children: [
                                // Icono representativo
                                Container(
                                  width: 48,
                                  height: 48,
                                  decoration: BoxDecoration(
                                    color: AppTheme.primary.withValues(alpha: 0.08),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Icon(iconData, color: AppTheme.primary, size: 26),
                                ),
                                const SizedBox(width: 14),

                                // Detalle del producto
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        item.producto,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 15,
                                          color: AppTheme.textPrimary,
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        item.servicio,
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary,
                                        ),
                                      ),
                                      if (item.legend != null && item.legend!.isNotEmpty) ...[
                                        const SizedBox(height: 4),
                                        Text(
                                          item.legend!,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(
                                            fontSize: 11,
                                            color: Colors.grey.shade600,
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                ),

                                const SizedBox(width: 8),

                                // Precio y acción
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: [
                                    Text(
                                      item.tienePrecioFijo
                                          ? Formatters.formatCurrency(item.precioFijoValor)
                                          : 'Monto libre',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 14,
                                        color: item.tienePrecioFijo
                                            ? AppTheme.accent
                                            : AppTheme.primary,
                                      ),
                                    ),
                                    const SizedBox(height: 6),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 10,
                                        vertical: 4,
                                      ),
                                      decoration: BoxDecoration(
                                        color: AppTheme.primary,
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: const Text(
                                        'Pagar',
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}
