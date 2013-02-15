#ifndef AIDL_ASTWRITER_H
#define AIDL_ASTWRITER_H

#include <stdio.h>
#include "AST.h"

using namespace std;

class ASTWriter {
public:
    ASTWriter(FILE* to_) : to(to_) { }

    virtual void WriteModifiers(int mod, int mask) = 0;
    virtual void WriteArgumentList(const vector<Expression*>& arguments) = 0;
    virtual void WriteLiteralExpression(LiteralExpression* e) = 0;
    virtual void WriteStringLiteralExpression(StringLiteralExpression* e) = 0;
    virtual void WriteVariable(Variable* e) = 0;
    virtual void WriteVariableDeclaration(Variable* e) = 0;
    virtual void WriteFieldVariable(FieldVariable* e) = 0;
    virtual void WriteField(Field* e) = 0;
    virtual void WriteStatementBlock(StatementBlock* e) = 0;
    virtual void WriteExpressionStatement(ExpressionStatement* e) = 0;
    virtual void WriteAssignment(Assignment* e) = 0;
    virtual void WriteMethodCall(MethodCall* e) = 0;
    virtual void WriteComparison(Comparison* e) = 0;
    virtual void WriteNewExpression(NewExpression* e) = 0;
    virtual void WriteNewArrayExpression(NewArrayExpression* e) = 0;
    virtual void WriteTernary(Ternary* e) = 0;
    virtual void WriteCast(Cast* e) = 0;
    virtual void WriteVariableDeclaration(VariableDeclaration* e) = 0;
    virtual void WriteIfStatement(IfStatement* e) = 0;
    virtual void WriteReturnStatement(ReturnStatement* e) = 0;
    virtual void WriteTryStatement(TryStatement* e) = 0;
    virtual void WriteCatchStatement(CatchStatement* e) = 0;
    virtual void WriteFinallyStatement(FinallyStatement* e) = 0;
    virtual void WriteSwitchStatement(SwitchStatement* e) = 0;
    virtual void WriteCase(Case* e) = 0;
    virtual void WriteBreak(Break* e) = 0;
    virtual void WriteMethod(Method* e) = 0;
    virtual void WriteClass(Class* e) = 0;
    virtual void WriteDocument(Document* e) = 0;

protected:
    FILE* to;
};

#endif
