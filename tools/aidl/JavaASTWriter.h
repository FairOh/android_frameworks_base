#ifndef AIDL_JAVAASTWRITER_H
#define AIDL_JAVAASTWRITER_H

#include "ASTWriter.h"

using namespace std;

class JavaASTWriter : public ASTWriter {
public:
    JavaASTWriter(FILE* out) : ASTWriter(out) { }

    void WriteModifiers(int mod, int mask);
    void WriteArgumentList(const vector<Expression*>& arguments);
    void WriteClassElement(ClassElement* e);
    void WriteExpression(Expression* e);
    void WriteLiteralExpression(LiteralExpression* e);
    void WriteStringLiteralExpression(StringLiteralExpression* e);
    void WriteVariableDeclaration(Variable* e);
    void WriteVariable(Variable* e);
    void WriteFieldVariable(FieldVariable* e);
    void WriteField(Field* e);
    void WriteStatementBlock(StatementBlock* e);
    void WriteExpressionStatement(ExpressionStatement* e);
    void WriteAssignment(Assignment* e);
    void WriteMethodCall(MethodCall* e);
    void WriteComparison(Comparison* e);
    void WriteNewExpression(NewExpression* e);
    void WriteNewArrayExpression(NewArrayExpression* e);
    void WriteTernary(Ternary* e);
    void WriteCast(Cast* e);
    void WriteVariableDeclaration(VariableDeclaration* e);
    void WriteIfStatement(IfStatement* e);
    void WriteReturnStatement(ReturnStatement* e);
    void WriteTryStatement(TryStatement* e);
    void WriteCatchStatement(CatchStatement* e);
    void WriteFinallyStatement(FinallyStatement* e);
    void WriteSwitchStatement(SwitchStatement* e);
    void WriteCase(Case* e);
    void WriteBreak(Break* e);
    void WriteMethod(Method* e);
    void WriteClass(Class* e);
    void WriteDocument(Document* e);
};

#endif
