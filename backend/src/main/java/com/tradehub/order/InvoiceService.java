package com.tradehub.order;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.payment.Payment;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
public class InvoiceService {

    private static final Color BRAND = new Color(0x1F, 0x3B, 0x73);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public InvoiceService(
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoice(String email, Long orderId) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph("TradeHub Invoice", titleFont));
            document.add(new Paragraph("Invoice #" + order.getId(), normalFont));
            document.add(new Paragraph("Ordered: " + order.getCreatedAt().withNano(0), normalFont));
            document.add(new Paragraph("Status: " + order.getStatus(), normalFont));

            Payment payment = order.getPayment();
            if (payment != null) {
                document.add(new Paragraph(
                        "Transaction: " + payment.getTransactionId(), normalFont));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Bill To", headerFont));
            document.add(new Paragraph(buildAddress(order), normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Items", headerFont));
            PdfPTable table = new PdfPTable(5);
            table.setWidths(new float[]{4f, 1f, 2f, 2f, 2f});

            table.addCell(headerCell("Product", headerFont));
            table.addCell(headerCell("Qty", headerFont));
            table.addCell(headerCell("Unit Price", headerFont));
            table.addCell(headerCell("Subtotal", headerFont));
            table.addCell(headerCell("", headerFont));

            for (OrderItem item : order.getItems()) {
                table.addCell(cell(item.getProductName(), normalFont));
                table.addCell(cell(item.getQuantity().toString(), normalFont));
                table.addCell(cell(item.getUnitPrice().toPlainString(), normalFont));
                table.addCell(cell(itemSubtotal(item).toPlainString(), normalFont));
                table.addCell(cell("", normalFont));
            }

            table.addCell(headerCell("Total", headerFont));
            table.addCell(cell("", normalFont));
            table.addCell(cell("", normalFont));
            table.addCell(cell("", normalFont));
            table.addCell(cell(order.getTotalAmount().toPlainString(), normalFont));

            document.add(table);
            document.close();

            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate invoice", exception);
        }
    }

    private String buildAddress(Order order) {
        StringBuilder builder = new StringBuilder();
        builder.append(order.getRecipientName());
        appendLine(builder, order.getAddressLine());
        appendLine(builder, order.getAddressLine2());
        appendLine(builder, order.getCity());
        appendLine(builder, order.getState());
        appendLine(builder, order.getZipCode());
        appendLine(builder, order.getCountry());
        appendLine(builder, order.getPhone());
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(", ").append(value);
        }
    }

    private BigDecimal itemSubtotal(OrderItem item) {
        return item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4);
        return cell;
    }
}